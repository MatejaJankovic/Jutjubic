package rs.ftn.isa.jutjubicbackend.service.strategy;

import org.springframework.stereotype.Component;

/**
 * Factory for selecting the appropriate TileAggregationStrategy based on zoom level.
 *
 * Zoom level ranges:
 * - 11-15 (detailed): Shows all videos, no clustering
 * - 6-10 (medium): Shows balanced selection, with clustering
 * - 1-5 (overview): Shows only representative videos, with clustering
 */
@Component
public class TileStrategyFactory {

    private final DetailedTileStrategy detailedTileStrategy;
    private final MediumTileStrategy mediumTileStrategy;
    private final OverviewTileStrategy overviewTileStrategy;

    public TileStrategyFactory(DetailedTileStrategy detailedTileStrategy,
                               MediumTileStrategy mediumTileStrategy,
                               OverviewTileStrategy overviewTileStrategy) {
        this.detailedTileStrategy = detailedTileStrategy;
        this.mediumTileStrategy = mediumTileStrategy;
        this.overviewTileStrategy = overviewTileStrategy;
    }

    /**
     * Returns the appropriate strategy for the given zoom level.
     *
     * @param zoom The map zoom level
     * @return The TileAggregationStrategy for this zoom level
     */
    public TileAggregationStrategy getStrategy(int zoom) {
        if (zoom >= 11) {
            return detailedTileStrategy;
        } else if (zoom >= 6) {
            return mediumTileStrategy;
        } else {
            return overviewTileStrategy;
        }
    }
}

