package rs.ftn.isa.jutjubicbackend.service.strategy;

import org.springframework.stereotype.Component;
import rs.ftn.isa.jutjubicbackend.model.Video;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Strategy for medium zoom levels (6-10).
 * Shows a balanced selection of popular and recent videos.
 * Maximum 50 videos per tile with clustering enabled.
 */
@Component
public class MediumTileStrategy implements TileAggregationStrategy {

    private static final int MAX_VIDEOS = 50; // Changed to 1 for testing purposes (original: 50)

    @Override
    public List<Long> selectVideos(List<Video> allVideos, int maxCount) {
        if (allVideos.size() <= MAX_VIDEOS) {
            // If total videos <= 50, return all
            return allVideos.stream()
                    .map(Video::getId)
                    .collect(Collectors.toList());
        }

        // 50% most popular (highest viewCount)
        // Ensure at least 1 video is selected from each category
        int halfCount = Math.max(1, MAX_VIDEOS / 2);

        List<Video> mostPopular = allVideos.stream()
                .sorted(Comparator.comparing(Video::getViewCount).reversed())
                .limit(halfCount)
                .collect(Collectors.toList());

        // 50% most recent (newest createdAt)
        List<Video> mostRecent = allVideos.stream()
                .sorted(Comparator.comparing(Video::getCreatedAt).reversed())
                .limit(halfCount)
                .collect(Collectors.toList());

        // Merge without duplicates using LinkedHashSet to maintain order
        Set<Long> mergedIds = new LinkedHashSet<>();

        mostPopular.forEach(v -> mergedIds.add(v.getId()));
        mostRecent.forEach(v -> mergedIds.add(v.getId()));

        return mergedIds.stream()
                .limit(MAX_VIDEOS)
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

