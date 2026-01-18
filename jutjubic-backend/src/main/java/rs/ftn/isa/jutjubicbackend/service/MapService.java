package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import rs.ftn.isa.jutjubicbackend.dto.VideoMarkerDTO;
import rs.ftn.isa.jutjubicbackend.model.Video;
import rs.ftn.isa.jutjubicbackend.repository.VideoRepository;
import rs.ftn.isa.jutjubicbackend.service.strategy.TileAggregationStrategy;
import rs.ftn.isa.jutjubicbackend.service.strategy.TileStrategyFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapService {

    private final VideoRepository videoRepository;
    private final CacheManager cacheManager;
    private final TileStrategyFactory tileStrategyFactory;

    /**
     * Dobavlja video markere za određeni viewport (bounds) mape
     * Koristi tile-based pristup za optimizaciju učitavanja podataka
     *
     * @param north Severna geografska širina
     * @param south Južna geografska širina
     * @param east Istočna geografska dužina
     * @param west Zapadna geografska dužina
     * @param zoom Zoom nivo mape
     * @return Lista video markera u zadatim granicama
     */
    @Cacheable(value = "mapTiles", key = "#north + '_' + #south + '_' + #east + '_' + #west + '_' + #zoom")
    public List<VideoMarkerDTO> getVideosForViewport(Double north, Double south,
                                                       Double east, Double west,
                                                       Integer zoom, String startDateStr, String endDateStr) {

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;

        try {
            if (startDateStr != null) startDate = LocalDate.parse(startDateStr, formatter).atStartOfDay();
            if (endDateStr != null) endDate = LocalDate.parse(endDateStr, formatter).atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Datumi nisu u ispravnom formatu yyyy-MM-dd");
        }

        List<Video> videos;

        if (startDate == null && endDate == null) {
            videos = videoRepository.findVideosInBounds(north, south, east, west);
        } else {

            if (startDate == null) {
                startDate = LocalDate.of(1970, 1, 1).atStartOfDay();
            }

            if (endDate == null) {
                endDate = LocalDateTime.now();
            }

            videos = videoRepository.findVideosInBoundsAndCreatedBetween(
                    north, south, east, west, startDate, endDate
            );
        }

        List<VideoMarkerDTO> markers = videos.stream()
                .map(VideoMarkerDTO::fromEntity)
                .collect(Collectors.toList());

        // Apply strategy pattern based on zoom level
        if (zoom != null) {
            TileAggregationStrategy strategy = tileStrategyFactory.getStrategy(zoom);

            // Log for debugging
            System.out.println("[MapService] Zoom: " + zoom +
                ", Strategy: " + strategy.getClass().getSimpleName() +
                ", MaxVideosPerTile: " + strategy.getMaxVideosPerTile() +
                ", TotalVideosInView: " + videos.size());

            // Get selected video IDs using the strategy
            List<Long> selectedVideoIds = strategy.selectVideos(videos, strategy.getMaxVideosPerTile());
            Set<Long> selectedIdSet = selectedVideoIds.stream().collect(Collectors.toSet());

            // Get representative video ID for clustering
            Long representativeVideoId = strategy.selectRepresentativeVideo(videos);

            // Log filtered results
            System.out.println("[MapService] Selected " + selectedVideoIds.size() + " videos after strategy filter");

            // Filter markers to only include selected videos
            markers = markers.stream()
                    .filter(m -> selectedIdSet.contains(m.getId()))
                    .collect(Collectors.toList());

            System.out.println("[MapService] Returning " + markers.size() + " markers to frontend");
        }

        return markers;
    }

    public void evictTileCache(String key) {
        if (cacheManager.getCache("mapTiles") != null) {
            cacheManager.getCache("mapTiles").evict(key);
        }
    }

    /**
     * Konvertuje geografske koordinate u tile koordinate za dati zoom nivo
     * Koristi Web Mercator projekciju (standardna za Leaflet/OSM)
     */
    public TileCoordinates latLngToTile(double lat, double lng, int zoom) {
        int x = (int) Math.floor((lng + 180.0) / 360.0 * Math.pow(2, zoom));
        int y = (int) Math.floor((1.0 - Math.log(Math.tan(Math.toRadians(lat)) +
                1.0 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2.0 * Math.pow(2, zoom));
        return new TileCoordinates(x, y, zoom);
    }

    public static class TileCoordinates {
        public final int x;
        public final int y;
        public final int zoom;

        public TileCoordinates(int x, int y, int zoom) {
            this.x = x;
            this.y = y;
            this.zoom = zoom;
        }
    }

    /**
     * Data class for storing tile aggregation information.
     * Contains selected video IDs, representative video, and metadata.
     */
    public static class TileData {
        public final int zoomLevel;
        public final int tileX;
        public final int tileY;
        public final List<Long> videoIds;
        public final int videoCount;
        public final Long representativeVideoId;
        public final LocalDateTime lastUpdated;
        public final boolean shouldCluster;

        public TileData(int zoomLevel, int tileX, int tileY, List<Long> videoIds,
                        int videoCount, Long representativeVideoId, boolean shouldCluster) {
            this.zoomLevel = zoomLevel;
            this.tileX = tileX;
            this.tileY = tileY;
            this.videoIds = videoIds;
            this.videoCount = videoCount;
            this.representativeVideoId = representativeVideoId;
            this.lastUpdated = LocalDateTime.now();
            this.shouldCluster = shouldCluster;
        }
    }

    /**
     * Creates or updates tile data for the given tile coordinates.
     * Uses the appropriate strategy based on zoom level to select videos.
     *
     * @param x Tile X coordinate
     * @param y Tile Y coordinate
     * @param zoom Zoom level
     * @return TileData containing selected videos and metadata
     */
    public TileData createOrUpdateTile(int x, int y, int zoom) {
        // Calculate bounding box for the tile
        double north = tileToLat(y, zoom);
        double south = tileToLat(y + 1, zoom);
        double west = tileToLng(x, zoom);
        double east = tileToLng(x + 1, zoom);

        // Get videos from repository using bounding box
        List<Video> videos = videoRepository.findVideosInBounds(north, south, east, west);

        // Choose strategy based on zoom level
        TileAggregationStrategy strategy = tileStrategyFactory.getStrategy(zoom);

        // Select video IDs using strategy
        List<Long> selectedVideoIds = strategy.selectVideos(videos, strategy.getMaxVideosPerTile());

        // Select representative video using strategy
        Long representativeVideoId = strategy.selectRepresentativeVideo(videos);

        // Store tile data with all required information
        return new TileData(
                zoom,                           // zoomLevel
                x,                              // tileX
                y,                              // tileY
                selectedVideoIds,               // videoIds (selected by strategy)
                videos.size(),                  // videoCount (ALL videos in tile)
                representativeVideoId,          // representativeVideoId
                strategy.shouldCluster()        // shouldCluster flag
        );
    }

    /**
     * Converts tile Y coordinate to latitude.
     */
    private double tileToLat(int y, int zoom) {
        double n = Math.PI - 2.0 * Math.PI * y / Math.pow(2, zoom);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    /**
     * Converts tile X coordinate to longitude.
     */
    private double tileToLng(int x, int zoom) {
        return x / Math.pow(2, zoom) * 360.0 - 180.0;
    }
}
