package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.isa.jutjubicbackend.dto.PopularVideoItemDTO;
import rs.ftn.isa.jutjubicbackend.dto.PopularVideosResponseDTO;
import rs.ftn.isa.jutjubicbackend.dto.VideoDTO;
import rs.ftn.isa.jutjubicbackend.model.PopularVideo;
import rs.ftn.isa.jutjubicbackend.model.Video;
import rs.ftn.isa.jutjubicbackend.model.VideoView;
import rs.ftn.isa.jutjubicbackend.repository.PopularVideoRepository;
import rs.ftn.isa.jutjubicbackend.repository.VideoRepository;
import rs.ftn.isa.jutjubicbackend.repository.VideoViewRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ETLService {

    private final VideoViewRepository videoViewRepository;
    private final PopularVideoRepository popularVideoRepository;
    private final VideoRepository videoRepository;

    /**
     * Runs the ETL pipeline to calculate popular videos based on the last 7 days of views
     */
    @Transactional
    public void runETLPipeline() {
        log.info("Starting ETL Pipeline for popular videos");

        try {
            // EXTRACT: Get views from the last 7 days
            List<VideoView> recentViews = extractRecentViews();
            log.info("Extracted {} views from the last 7 days", recentViews.size());

            // TRANSFORM: Calculate popularity scores
            Map<Video, Double> popularityScores = transformToPopularityScores(recentViews);
            log.info("Calculated popularity scores for {} videos", popularityScores.size());

            // Get top 3 videos
            List<Map.Entry<Video, Double>> top3 = popularityScores.entrySet().stream()
                    .sorted(Map.Entry.<Video, Double>comparingByValue().reversed())
                    .limit(3)
                    .toList();

            // LOAD: Save results to database
            loadResults(top3);
            log.info("ETL Pipeline completed successfully");

        } catch (Exception e) {
            log.error("Error running ETL Pipeline", e);
            throw new RuntimeException("ETL Pipeline failed", e);
        }
    }

    /**
     * EXTRACT phase: Retrieve video views from the last 7 days
     */
    private List<VideoView> extractRecentViews() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return videoViewRepository.findViewsSince(sevenDaysAgo);
    }

    /**
     * TRANSFORM phase: Group views by video and calculate popularity score
     * Score formula: For each day in the last 7 days, views are multiplied by weight (7 - daysAgo + 1)
     * - Views from today (0 days ago): weight = 7
     * - Views from yesterday (1 day ago): weight = 6
     * - Views from 6 days ago: weight = 1
     */
    private Map<Video, Double> transformToPopularityScores(List<VideoView> views) {
        Map<Video, Double> scores = new HashMap<>();
        LocalDate today = LocalDate.now();

        for (VideoView view : views) {
            Video video = view.getVideo();
            LocalDate viewDate = view.getViewedAt().toLocalDate();

            // Calculate how many days ago this view occurred
            long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(viewDate, today);

            // Calculate weight: views from today get weight 7, yesterday 6, ..., 7 days ago get weight 1
            double weight = 7 - daysAgo;

            // Only count views from the last 7 days (weight should be between 1 and 7)
            if (weight >= 1 && weight <= 7) {
                scores.merge(video, weight, Double::sum);
            }
        }

        return scores;
    }

    /**
     * LOAD phase: Save the top 3 popular videos to the database
     */
    private void loadResults(List<Map.Entry<Video, Double>> top3) {
        PopularVideo.PopularVideoBuilder builder = PopularVideo.builder()
                .pipelineRunAt(LocalDateTime.now());

        if (!top3.isEmpty()) {
            builder.firstVideo(top3.get(0).getKey())
                   .firstVideoScore(top3.get(0).getValue());
        }
        if (top3.size() > 1) {
            builder.secondVideo(top3.get(1).getKey())
                   .secondVideoScore(top3.get(1).getValue());
        }
        if (top3.size() > 2) {
            builder.thirdVideo(top3.get(2).getKey())
                   .thirdVideoScore(top3.get(2).getValue());
        }

        PopularVideo popularVideo = builder.build();
        popularVideoRepository.save(popularVideo);

        log.info("Saved popular videos: 1st={} (score={}), 2nd={} (score={}), 3rd={} (score={})",
                popularVideo.getFirstVideo() != null ? popularVideo.getFirstVideo().getTitle() : "N/A",
                popularVideo.getFirstVideoScore(),
                popularVideo.getSecondVideo() != null ? popularVideo.getSecondVideo().getTitle() : "N/A",
                popularVideo.getSecondVideoScore(),
                popularVideo.getThirdVideo() != null ? popularVideo.getThirdVideo().getTitle() : "N/A",
                popularVideo.getThirdVideoScore());
    }

    /**
     * Get the latest popular videos result
     */
    public Optional<PopularVideosResponseDTO> getLatestPopularVideos() {
        return popularVideoRepository.findLatest()
                .map(this::toDTO);
    }

    private PopularVideosResponseDTO toDTO(PopularVideo popularVideo) {
        List<PopularVideoItemDTO> topVideos = new ArrayList<>();

        if (popularVideo.getFirstVideo() != null) {
            topVideos.add(PopularVideoItemDTO.builder()
                    .video(VideoDTO.fromEntity(popularVideo.getFirstVideo()))
                    .score(popularVideo.getFirstVideoScore())
                    .build());
        }
        if (popularVideo.getSecondVideo() != null) {
            topVideos.add(PopularVideoItemDTO.builder()
                    .video(VideoDTO.fromEntity(popularVideo.getSecondVideo()))
                    .score(popularVideo.getSecondVideoScore())
                    .build());
        }
        if (popularVideo.getThirdVideo() != null) {
            topVideos.add(PopularVideoItemDTO.builder()
                    .video(VideoDTO.fromEntity(popularVideo.getThirdVideo()))
                    .score(popularVideo.getThirdVideoScore())
                    .build());
        }

        return PopularVideosResponseDTO.builder()
                .pipelineRunAt(popularVideo.getPipelineRunAt())
                .topVideos(topVideos)
                .build();
    }
}



