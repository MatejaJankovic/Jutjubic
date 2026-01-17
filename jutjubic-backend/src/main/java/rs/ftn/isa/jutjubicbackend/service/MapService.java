package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import rs.ftn.isa.jutjubicbackend.dto.VideoMarkerDTO;
import rs.ftn.isa.jutjubicbackend.model.Video;
import rs.ftn.isa.jutjubicbackend.repository.VideoRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapService {

    private final VideoRepository videoRepository;
    private final CacheManager cacheManager;

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

        if (zoom != null && zoom < 6) {
            return markers.stream()
                    .sorted((v1, v2) -> Long.compare(v2.getViewCount(), v1.getViewCount()))
                    .limit(50)
                    .collect(Collectors.toList());
        } else if (zoom != null && zoom < 11) {
            return markers.stream()
                    .limit(200)
                    .collect(Collectors.toList());
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
}

