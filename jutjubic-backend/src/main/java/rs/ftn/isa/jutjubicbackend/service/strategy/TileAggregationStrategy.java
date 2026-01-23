package rs.ftn.isa.jutjubicbackend.service.strategy;

import rs.ftn.isa.jutjubicbackend.model.Video;

import java.util.List;

/**
 * Strategy interface for aggregating videos inside map tiles based on zoom level.
 * Different zoom levels require different aggregation strategies:
 * - Overview (1-5): Show only a few representative videos
 * - Medium (6-10): Show a balanced selection of popular and recent videos
 * - Detailed (11-15): Show all videos without filtering
 */
public interface TileAggregationStrategy {

    /**
     * Selects video IDs from the given list based on the strategy's rules.
     *
     * @param allVideos List of all videos in the tile
     * @param maxCount Maximum number of videos to select (may be ignored by some strategies)
     * @return List of selected video IDs
     */
    List<Long> selectVideos(List<Video> allVideos, int maxCount);

    /**
     * Selects the representative video for a tile (used for clustering markers).
     *
     * @param videos List of videos in the tile
     * @return ID of the representative video, or null if no videos
     */
    Long selectRepresentativeVideo(List<Video> videos);

    /**
     * Returns the maximum number of videos that should be displayed per tile.
     *
     * @return Maximum video count for this strategy
     */
    int getMaxVideosPerTile();

    /**
     * Indicates whether videos should be clustered at this zoom level.
     *
     * @return true if clustering should be applied, false otherwise
     */
    boolean shouldCluster();
}

