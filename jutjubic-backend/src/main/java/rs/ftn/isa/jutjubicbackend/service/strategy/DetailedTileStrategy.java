package rs.ftn.isa.jutjubicbackend.service.strategy;

import org.springframework.stereotype.Component;
import rs.ftn.isa.jutjubicbackend.model.Video;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Strategy for detailed zoom levels (11-15).
 * Shows all videos without any filtering or limiting.
 * No clustering is applied at this zoom level.
 */
@Component
public class DetailedTileStrategy implements TileAggregationStrategy {

    @Override
    public List<Long> selectVideos(List<Video> allVideos, int maxCount) {
        // Return ALL video IDs without any limit
        return allVideos.stream()
                .map(Video::getId)
                .collect(Collectors.toList());
    }

    @Override
    public Long selectRepresentativeVideo(List<Video> videos) {
        // Return the newest video (max createdAt)
        return videos.stream()
                .max(Comparator.comparing(Video::getCreatedAt))
                .map(Video::getId)
                .orElse(null);
    }

    @Override
    public int getMaxVideosPerTile() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean shouldCluster() {
        return false;
    }
}

