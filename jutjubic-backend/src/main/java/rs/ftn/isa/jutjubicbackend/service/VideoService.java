package rs.ftn.isa.jutjubicbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.ftn.isa.jutjubicbackend.dto.CreateVideoRequest;
import rs.ftn.isa.jutjubicbackend.dto.VideoDTO;
import rs.ftn.isa.jutjubicbackend.dto.VideoPageResponse;
import rs.ftn.isa.jutjubicbackend.model.User;
import rs.ftn.isa.jutjubicbackend.model.Video;
import rs.ftn.isa.jutjubicbackend.model.VideoLike;
import rs.ftn.isa.jutjubicbackend.repository.UserRepository;
import rs.ftn.isa.jutjubicbackend.repository.VideoLikeRepository;
import rs.ftn.isa.jutjubicbackend.repository.VideoRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final MapService mapService;
    private final CacheManager cacheManager;
    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Korisnik nije autentifikovan");
        }

        Object principal = auth.getPrincipal();

        String email;

        if (principal instanceof User u) {
            email = u.getEmail();
        } else {
            email = auth.getName();
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Trenutni korisnik nije pronađen"));
    }

    private User getCurrentUserOrNull() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }

            Object principal = auth.getPrincipal();
            String email;

            if (principal instanceof User u) {
                email = u.getEmail();
            } else {
                email = auth.getName();
            }

            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private VideoDTO toVideoDTO(Video video) {
        VideoDTO dto = VideoDTO.fromEntity(video);
        User currentUser = getCurrentUserOrNull();
        if (currentUser != null) {
            dto.setLikedByCurrentUser(
                videoLikeRepository.existsByVideoIdAndUserId(video.getId(), currentUser.getId())
            );
        }
        return dto;
    }

    public VideoPageResponse getAllVideos(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videoPage = videoRepository.findAllByOrderByCreatedAtDesc(pageable);

        return VideoPageResponse.builder()
                .videos(videoPage.getContent().stream().map(this::toVideoDTO).toList())
                .currentPage(videoPage.getNumber())
                .totalPages(videoPage.getTotalPages())
                .totalElements(videoPage.getTotalElements())
                .hasNext(videoPage.hasNext())
                .hasPrevious(videoPage.hasPrevious())
                .build();
    }

    public Optional<VideoDTO> getVideoById(Long id) {
        return videoRepository.findById(id).map(this::toVideoDTO);
    }

    @Transactional
    public Optional<VideoDTO> incrementViewCount(Long id) {
        int updatedRows = videoRepository.incrementViewCountById(id);
        if (updatedRows > 0) {
            return videoRepository.findById(id).map(this::toVideoDTO);
        }
        return Optional.empty();
    }

    @Transactional
    public VideoDTO toggleLike(Long videoId) {
        User currentUser = getCurrentUser();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video nije pronađen"));

        boolean alreadyLiked = videoLikeRepository.existsByVideoIdAndUserId(videoId, currentUser.getId());

        if (alreadyLiked) {
            // Unlike
            videoLikeRepository.deleteByVideoIdAndUserId(videoId, currentUser.getId());
            videoRepository.decrementLikeCountById(videoId);
        } else {
            // Like
            VideoLike like = VideoLike.builder()
                    .video(video)
                    .user(currentUser)
                    .build();
            videoLikeRepository.save(like);
            videoRepository.incrementLikeCountById(videoId);
        }

        // Refresh and return updated video
        return videoRepository.findById(videoId)
                .map(this::toVideoDTO)
                .orElseThrow(() -> new RuntimeException("Video nije pronađen"));
    }

    public VideoPageResponse searchVideos(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videoPage = videoRepository.searchByTitle(query, pageable);

        return VideoPageResponse.builder()
                .videos(videoPage.getContent().stream().map(this::toVideoDTO).toList())
                .currentPage(videoPage.getNumber())
                .totalPages(videoPage.getTotalPages())
                .totalElements(videoPage.getTotalElements())
                .hasNext(videoPage.hasNext())
                .hasPrevious(videoPage.hasPrevious())
                .build();
    }

    public VideoPageResponse getTrendingVideos(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videoPage = videoRepository.findTrending(pageable);

        return VideoPageResponse.builder()
                .videos(videoPage.getContent().stream().map(this::toVideoDTO).toList())
                .currentPage(videoPage.getNumber())
                .totalPages(videoPage.getTotalPages())
                .totalElements(videoPage.getTotalElements())
                .hasNext(videoPage.hasNext())
                .hasPrevious(videoPage.hasPrevious())
                .build();
    }

    @Transactional
    public VideoDTO createVideo(CreateVideoRequest request,
                                MultipartFile video,
                                MultipartFile thumbnail) {

        validateVideo(video);

        Path videoPath = null;
        Path thumbPath = null;

        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path videoDir = base.resolve("videos");
            Path thumbDir = base.resolve("thumbnails");

            Files.createDirectories(videoDir);
            Files.createDirectories(thumbDir);

            String videoName = UUID.randomUUID() + ".mp4";
            String thumbName = UUID.randomUUID() + ".jpg";

            videoPath = videoDir.resolve(videoName);
            thumbPath = thumbDir.resolve(thumbName);

            uploadWithTimeout(video, videoPath);
            uploadWithTimeout(thumbnail, thumbPath);

            Video videoEntity = Video.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .tags(request.getTags())
                    .location(request.getLocation())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .videoUrl("/uploads/videos/" + videoName)
                    .thumbnailUrl("/uploads/thumbnails/" + thumbName)
                    .user(getCurrentUser())
                    .viewCount(0L)
                    .likeCount(0L)
                    .commentCount(0L)
                    .build();

            videoRepository.save(videoEntity);

            var tile = mapService.latLngToTile(videoEntity.getLatitude(), videoEntity.getLongitude(), 12); // npr. zoom 12

            double north = mapService.tileToLat(tile.y, tile.zoom);
            double south = mapService.tileToLat(tile.y + 1, tile.zoom);
            double west  = mapService.tileToLng(tile.x, tile.zoom);
            double east  = mapService.tileToLng(tile.x + 1, tile.zoom);

            String cacheKey = north + "_" + south + "_" + east + "_" + west + "_" + tile.zoom;
            mapService.evictTileCache(cacheKey);


            return VideoDTO.fromEntity(videoEntity);

        } catch (Exception e) {
            try { if (videoPath != null) Files.deleteIfExists(videoPath); } catch (Exception ignored) {}
            try { if (thumbPath != null) Files.deleteIfExists(thumbPath); } catch (Exception ignored) {}

            throw new RuntimeException("Upload neuspešan — rollback", e);
        }
    }

    private void uploadWithTimeout(MultipartFile file, Path target) throws Exception {
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();

        try {
            var future = executor.submit(() -> {
                try {
                    Files.copy(file.getInputStream(), target);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return true;
            });

            future.get(120, java.util.concurrent.TimeUnit.SECONDS);

        } catch (java.util.concurrent.TimeoutException e) {
            throw new RuntimeException("Upload traje predugo");
        } finally {
            executor.shutdownNow();
        }
    }


    private void validateVideo(MultipartFile video) {
        if (!"video/mp4".equals(video.getContentType())) {
            throw new IllegalArgumentException("Dozvoljen je samo mp4 format");
        }

        if (video.getSize() > 200 * 1024 * 1024) {
            throw new IllegalArgumentException("Video ne sme biti veći od 200MB");
        }
    }

    @Cacheable(value = "thumbnails", key = "#id")
    public byte[] getThumbnail(Long id) throws IOException {
        Video video = videoRepository.findById(id).orElseThrow();

        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path thumbDir = base.resolve("thumbnails");

        Path realPath = thumbDir.resolve(
                Paths.get(video.getThumbnailUrl()).getFileName().toString()
        );

        return Files.readAllBytes(realPath);
    }
}

