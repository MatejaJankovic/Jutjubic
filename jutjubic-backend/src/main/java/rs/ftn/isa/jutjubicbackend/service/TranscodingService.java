package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.isa.jutjubicbackend.dto.TranscodingMessage;
import rs.ftn.isa.jutjubicbackend.model.VideoVariant;
import rs.ftn.isa.jutjubicbackend.repository.VideoVariantRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for processing video transcoding using FFmpeg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranscodingService {

    private final VideoVariantRepository videoVariantRepository;

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Process video transcoding request
     * Creates 720p and 1080p variants using FFmpeg
     *
     * @param message Transcoding message containing video details
     * @throws Exception if transcoding fails
     */
    @Transactional
    public void process(TranscodingMessage message) throws Exception {
        Long videoId = message.getVideoId();
        String inputPath = message.getInputPath();

        log.info("Starting transcoding for videoId={}, inputPath={}, profiles={}",
                videoId, inputPath, message.getRequestedProfiles());

        // Validate input file exists
        Path inputFile = Paths.get(inputPath);
        if (!Files.exists(inputFile)) {
            throw new IllegalArgumentException("Input file does not exist: " + inputPath);
        }

        // Create transcoded directory
        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path transcodedDir = baseDir.resolve("transcoded");
        Files.createDirectories(transcodedDir);

        // Process each profile
        List<String> profiles = message.getRequestedProfiles();
        if (profiles == null || profiles.isEmpty()) {
            profiles = List.of("720p", "1080p");
        }

        for (String profile : profiles) {
            long startTime = System.currentTimeMillis();

            try {
                String outputFileName = videoId + "_" + profile + ".mp4";
                Path outputPath = transcodedDir.resolve(outputFileName);

                log.info("Transcoding video {} to profile {}...", videoId, profile);

                // Transcode the video
                transcodeVideo(inputFile, outputPath, profile);

                // Save variant to database
                VideoVariant variant = VideoVariant.builder()
                        .videoId(videoId)
                        .profile(profile)
                        .filePath("/uploads/transcoded/" + outputFileName)
                        .build();

                videoVariantRepository.save(variant);

                long duration = System.currentTimeMillis() - startTime;
                log.info("Transcoding completed for videoId={}, profile={}, duration={}ms",
                        videoId, profile, duration);

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("Transcoding failed for videoId={}, profile={}, duration={}ms, error={}",
                        videoId, profile, duration, e.getMessage(), e);
                throw e; // Rethrow to mark job as FAILED
            }
        }

        log.info("All transcoding completed for videoId={}", videoId);
    }

    /**
     * Transcode video to specified profile using FFmpeg
     */
    private void transcodeVideo(Path inputPath, Path outputPath, String profile) throws Exception {
        // Create temporary output file with proper .mp4 extension (FFmpeg needs this)
        String tempFileName = outputPath.getFileName().toString().replace(".mp4", "_temp.mp4");
        Path tempOutput = outputPath.getParent().resolve(tempFileName);

        // Build FFmpeg command based on profile
        List<String> command = buildFFmpegCommand(inputPath.toString(), tempOutput.toString(), profile);

        log.debug("Executing FFmpeg command: {}", String.join(" ", command));

        try {
            // Execute FFmpeg process
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr

            Process process = processBuilder.start();

            // Capture output for error reporting
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Log progress (optional - FFmpeg outputs progress to stderr)
                    if (line.contains("time=") || line.contains("frame=")) {
                        log.trace("FFmpeg: {}", line);
                    }
                }
            }

            // Wait for process to complete (max 30 minutes)
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("FFmpeg process timeout (exceeded 30 minutes)");
            }

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                // Extract last 20 lines of output for error message
                String[] lines = output.toString().split("\n");
                int start = Math.max(0, lines.length - 20);
                StringBuilder lastLines = new StringBuilder();
                for (int i = start; i < lines.length; i++) {
                    lastLines.append(lines[i]).append("\n");
                }

                throw new RuntimeException(
                        "FFmpeg process failed with exit code " + exitCode + ". Last output:\n" + lastLines);
            }

            // Atomic move from temp to final location
            Files.move(tempOutput, outputPath, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            // Clean up temporary file if it exists
            try {
                Files.deleteIfExists(tempOutput);
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    /**
     * Build FFmpeg command for specified profile
     */
    private List<String> buildFFmpegCommand(String inputPath, String outputPath, String profile) {
        List<String> command = new ArrayList<>();

        command.add(ffmpegPath);
        command.add("-y"); // Overwrite output file if exists
        command.add("-i");
        command.add(inputPath);

        // Add profile-specific parameters
        switch (profile.toLowerCase()) {
            case "720p":
                command.add("-vf");
                command.add("scale=-2:720"); // Maintain aspect ratio, height=720
                break;

            case "1080p":
                command.add("-vf");
                command.add("scale=-2:1080"); // Maintain aspect ratio, height=1080
                break;

            default:
                throw new IllegalArgumentException("Unsupported profile: " + profile);
        }

        // Common encoding parameters
        command.add("-c:v");
        command.add("libx264"); // Video codec
        command.add("-preset");
        command.add("veryfast"); // Encoding speed/quality trade-off
        command.add("-crf");
        command.add("23"); // Constant Rate Factor (quality: 0-51, lower=better)
        command.add("-c:a");
        command.add("aac"); // Audio codec
        command.add("-b:a");
        command.add("128k"); // Audio bitrate

        // Output file
        command.add(outputPath);

        return command;
    }
}

