package rs.ftn.isa.jutjubicbackend.service.strategy;

import org.springframework.stereotype.Component;
import rs.ftn.isa.jutjubicbackend.model.Video;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy for overview zoom levels (1-5).
 * Shows top 20 most viewed videos per tile.
 * Videos are selected based on viewCount (highest first).
 * Clustering is enabled.
 */
@Component
public class OverviewTileStrategy implements TileAggregationStrategy {

    private static final int MAX_VIDEOS = 20; // Shows top 20 most viewed videos at low zoom

    @Override
    public List<Long> selectVideos(List<Video> allVideos, int maxCount) {
        if (allVideos.isEmpty()) {
            return Collections.emptyList();
        }

        // If we have fewer videos than MAX_VIDEOS, return all
        if (allVideos.size() <= MAX_VIDEOS) {
            return allVideos.stream()
                    .map(Video::getId)
                    .collect(Collectors.toList());
        }

        // Select top MAX_VIDEOS most viewed videos
        return allVideos.stream()
                .sorted(Comparator.comparing(Video::getViewCount).reversed())
                .limit(MAX_VIDEOS)
                .map(Video::getId)
                .collect(Collectors.toList());
    }

    @Override
    public Long selectRepresentativeVideo(List<Video> videos) {
        // Return the most popular video (max viewCount)
        return videos.stream()
                .max(Comparator.comparing(Video::getViewCount))
                .map(Video::getId)
                .orElse(null);
    }

    @Override
    public int getMaxVideosPerTile() {
        return MAX_VIDEOS;
    }

    @Override
    public boolean shouldCluster() {
        return true;
    }
}

