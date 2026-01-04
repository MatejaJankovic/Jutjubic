package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.isa.jutjubicbackend.dto.VideoDTO;
import rs.ftn.isa.jutjubicbackend.dto.VideoPageResponse;
import rs.ftn.isa.jutjubicbackend.model.Video;
import rs.ftn.isa.jutjubicbackend.repository.VideoRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;

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
}

