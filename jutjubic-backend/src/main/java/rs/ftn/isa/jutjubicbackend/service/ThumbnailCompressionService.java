package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.isa.jutjubicbackend.model.Video;
import rs.ftn.isa.jutjubicbackend.repository.VideoRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailCompressionService {

    private final VideoRepository videoRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Scheduled(cron = "0 0 2 * * ?") // every day at 02:00
    @Transactional
    public void compressOldThumbnails() {
        long startTime = System.currentTimeMillis();

        log.info("Starting scheduled thumbnail compression task...");

        // 1) Calculate threshold = Instant.now().minus(30, ChronoUnit.DAYS)
        Instant thresholdInstant = Instant.now().minus(30, ChronoUnit.DAYS);
        LocalDateTime threshold = LocalDateTime.ofInstant(thresholdInstant, ZoneId.systemDefault());
        log.info("Threshold date: {} (thumbnails older than 30 days)", threshold);

        // 2) Fetch all videos with uncompressed thumbnails older than threshold
        List<Video> videosToCompress = videoRepository.findByThumbnailCompressedFalseAndCreatedAtBefore(threshold);

        log.info("Found {} thumbnails to compress", videosToCompress.size());

        if (videosToCompress.isEmpty()) {
            log.info("No thumbnails to compress. Task completed.");
            return;
        }

        int successCount = 0;
        int failureCount = 0;
        int skippedExternal = 0;
        int skippedMissing = 0;

        // 3) For each video, compress the thumbnail
        for (Video video : videosToCompress) {
            try {
                compressThumbnail(video);
                successCount++;
                log.info("Successfully compressed thumbnail for video ID: {}", video.getId());
            } catch (IllegalStateException e) {
                String message = e.getMessage();
                if (message.contains("external thumbnail URL")) {
                    skippedExternal++;
                    log.warn("Skipped video ID {} - External URL", video.getId());
                } else if (message.contains("does not exist")) {
                    skippedMissing++;
                    log.warn("Skipped video ID {} - File missing", video.getId());
                } else {
                    failureCount++;
                    log.error("Failed to compress thumbnail for video ID: {} - {}", video.getId(), e.getMessage());
                }
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to compress thumbnail for video ID: {}", video.getId(), e);
                // Continue with next video
            }
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // 5) Add logging summary
        log.info("Thumbnail compression task completed:");
        log.info("  - Total thumbnails found: {}", videosToCompress.size());
        log.info("  - Successfully compressed: {}", successCount);
        log.info("  - Skipped (external URLs): {}", skippedExternal);
        log.info("  - Skipped (missing files): {}", skippedMissing);
        log.info("  - Failed (errors): {}", failureCount);
        log.info("  - Total execution time: {} ms ({} seconds)", executionTime, executionTime / 1000);
    }

    /**
     * Compress ALL uncompressed thumbnails regardless of age (for testing/debugging)
     */
    @Transactional
    public void compressAllUncompressedThumbnails() {
        long startTime = System.currentTimeMillis();

        log.info("Starting compression of ALL uncompressed thumbnails (no age filter)...");

        // Fetch ALL videos with uncompressed thumbnails (no date filter)
        List<Video> videosToCompress = videoRepository.findByThumbnailCompressedFalse();

        log.info("Found {} uncompressed thumbnails", videosToCompress.size());

        if (videosToCompress.isEmpty()) {
            log.info("No uncompressed thumbnails found. Task completed.");
            return;
        }

        int successCount = 0;
        int failureCount = 0;
        int skippedExternal = 0;
        int skippedMissing = 0;

        for (Video video : videosToCompress) {
            try {
                compressThumbnail(video);
                successCount++;
                log.info("Successfully compressed thumbnail for video ID: {} ({})", video.getId(), video.getTitle());
            } catch (IllegalStateException e) {
                String message = e.getMessage();
                if (message.contains("external thumbnail URL")) {
                    skippedExternal++;
                    log.warn("Skipped video ID {} - External URL: {}", video.getId(), video.getThumbnailUrl());
                } else if (message.contains("does not exist")) {
                    skippedMissing++;
                    log.warn("Skipped video ID {} - File missing: {}", video.getId(), message);
                } else {
                    failureCount++;
                    log.error("Failed to compress thumbnail for video ID: {} - {}", video.getId(), e.getMessage());
                }
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to compress thumbnail for video ID: {} - {}", video.getId(), e.getMessage(), e);
            }
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        log.info("All thumbnails compression task completed:");
        log.info("  - Total thumbnails found: {}", videosToCompress.size());
        log.info("  - Successfully compressed: {}", successCount);
        log.info("  - Skipped (external URLs): {}", skippedExternal);
        log.info("  - Skipped (missing files): {}", skippedMissing);
        log.info("  - Failed (errors): {}", failureCount);
        log.info("  - Total execution time: {} ms ({} seconds)", executionTime, executionTime / 1000);
    }

    private void compressThumbnail(Video video) throws Exception {
        // Verify thumbnail exists
        if (video.getThumbnailUrl() == null || video.getThumbnailUrl().isEmpty()) {
            throw new IllegalStateException("Video has no thumbnail URL");
        }

        // Skip external URLs (e.g., https://picsum.photos/...)
        if (video.getThumbnailUrl().startsWith("http://") || video.getThumbnailUrl().startsWith("https://")) {
            throw new IllegalStateException("Cannot compress external thumbnail URL: " + video.getThumbnailUrl());
        }

        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path thumbnailsDir = base.resolve("thumbnails");

        // Extract filename from thumbnail URL (e.g., "/uploads/thumbnails/uuid.jpg" -> "uuid.jpg")
        String thumbnailFileName = Paths.get(video.getThumbnailUrl()).getFileName().toString();
        Path inputFile = thumbnailsDir.resolve(thumbnailFileName);

        // Verify thumbnail file exists
        if (!Files.exists(inputFile)) {
            throw new IllegalStateException("Thumbnail file does not exist: " + inputFile);
        }

        // Create compressed folder
        Path compressedDir = base.resolve("compressed");
        Files.createDirectories(compressedDir);

        // Generate output filename: <videoId>_compressed.jpg
        String outputFileName = video.getId() + "_compressed.jpg";
        Path outputFile = compressedDir.resolve(outputFileName);

        // Compress using Thumbnailator
        Thumbnails.of(inputFile.toFile())
                .scale(1.0)
                .outputQuality(0.7)
                .toFile(outputFile.toFile());

        // Update video entity
        video.setThumbnailCompressed(true);
        video.setCompressedThumbnailPath("/uploads/compressed/" + outputFileName);

        // Save video (will be saved by @Transactional on method completion)
        videoRepository.save(video);

        // 4) Do NOT delete original thumbnail
        log.debug("Compressed thumbnail saved to: {}", outputFile);
    }
}

