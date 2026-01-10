package rs.ftn.isa.jutjubicbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import rs.ftn.isa.jutjubicbackend.repository.UserRepository;
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

    public VideoPageResponse getAllVideos(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videoPage = videoRepository.findAllByOrderByCreatedAtDesc(pageable);

        return VideoPageResponse.builder()
                .videos(videoPage.getContent().stream().map(VideoDTO::fromEntity).toList())
                .currentPage(videoPage.getNumber())
                .totalPages(videoPage.getTotalPages())
                .totalElements(videoPage.getTotalElements())
                .hasNext(videoPage.hasNext())
                .hasPrevious(videoPage.hasPrevious())
                .build();
    }

    public Optional<VideoDTO> getVideoById(Long id) {
        return videoRepository.findById(id).map(VideoDTO::fromEntity);
    }

    @Transactional
    public Optional<VideoDTO> incrementViewCount(Long id) {
        Optional<Video> videoOptional = videoRepository.findById(id);
        if (videoOptional.isPresent()) {
            Video video = videoOptional.get();
            video.setViewCount(video.getViewCount() + 1);
            videoRepository.save(video);
            return Optional.of(VideoDTO.fromEntity(video));
        }
        return Optional.empty();
    }

    public VideoPageResponse searchVideos(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videoPage = videoRepository.searchByTitle(query, pageable);

        return VideoPageResponse.builder()
                .videos(videoPage.getContent().stream().map(VideoDTO::fromEntity).toList())
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
                .videos(videoPage.getContent().stream().map(VideoDTO::fromEntity).toList())
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

        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path videoDir = base.resolve("videos");
            Path thumbDir = base.resolve("thumbnails");

            Files.createDirectories(videoDir);
            Files.createDirectories(thumbDir);

            String videoName = UUID.randomUUID() + ".mp4";
            String thumbName = UUID.randomUUID() + ".jpg";

            Path videoPath = videoDir.resolve(videoName);
            Path thumbPath = thumbDir.resolve(thumbName);

            Files.copy(video.getInputStream(), videoPath);
            Files.copy(thumbnail.getInputStream(), thumbPath);

            Video videoEntity = Video.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .tags(request.getTags())
                    .location(request.getLocation())
                    .videoUrl("/uploads/videos/" + videoName)
                    .thumbnailUrl("/uploads/thumbnails/" + thumbName)
                    .user(getCurrentUser())
                    .viewCount(0L)
                    .likeCount(0L)
                    .commentCount(0L)
                    .build();


            videoRepository.save(videoEntity);

            return VideoDTO.fromEntity(videoEntity);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Upload neuspešan — rollback transakcije");
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
        return Files.readAllBytes(Paths.get(video.getThumbnailUrl()));
    }

}

