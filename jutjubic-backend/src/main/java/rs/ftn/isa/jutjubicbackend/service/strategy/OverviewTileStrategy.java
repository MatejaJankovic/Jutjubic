package rs.ftn.isa.jutjubicbackend.service.strategy;

import org.springframework.stereotype.Component;
import rs.ftn.isa.jutjubicbackend.model.Video;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy for overview zoom levels (1-5).
 * Shows only 3 representative videos per tile.
 * Selection: most popular, most recent (different from popular), and one random.
 * Clustering is enabled.
 */
@Component
public class OverviewTileStrategy implements TileAggregationStrategy {

    private static final int MAX_VIDEOS = 3; // Changed to 1 for testing purposes (original: 3)
    private final Random random = new Random();

    @Override
    public List<Long> selectVideos(List<Video> allVideos, int maxCount) {
        if (allVideos.isEmpty()) {
            return Collections.emptyList();
        }

        if (allVideos.size() <= MAX_VIDEOS) {
            return allVideos.stream()
                    .map(Video::getId)
                    .collect(Collectors.toList());
        }

        Set<Long> selectedIds = new LinkedHashSet<>();

        // 1) Most popular (highest viewCount)
        if (selectedIds.size() < MAX_VIDEOS) {
            Video mostPopular = allVideos.stream()
                    .max(Comparator.comparing(Video::getViewCount))
                    .orElse(null);

            if (mostPopular != null) {
                selectedIds.add(mostPopular.getId());
            }
        }

        // 2) Most recent (newest createdAt), different from the most popular
        if (selectedIds.size() < MAX_VIDEOS) {
            Video mostRecent = allVideos.stream()
                    .filter(v -> !selectedIds.contains(v.getId()))
                    .max(Comparator.comparing(Video::getCreatedAt))
                    .orElse(null);

            if (mostRecent != null) {
                selectedIds.add(mostRecent.getId());
            }
        }

        // 3) Random remaining (if exists and we still have room)
        if (selectedIds.size() < MAX_VIDEOS) {
            List<Video> remaining = allVideos.stream()
                    .filter(v -> !selectedIds.contains(v.getId()))
                    .collect(Collectors.toList());

            if (!remaining.isEmpty()) {
                Video randomVideo = remaining.get(random.nextInt(remaining.size()));
                selectedIds.add(randomVideo.getId());
            }
        }

        return new ArrayList<>(selectedIds);
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

