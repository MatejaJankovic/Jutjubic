package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TileRecomputeService {

    private final MapService mapService;

    @Scheduled(cron = "0 0 2 * * *") // svaku noć u 02:00
    public void recomputeAllTiles() {
        List<Integer> zoomLevels = List.of(6, 11, 15);
        for (int zoom : zoomLevels) {
            int maxTile = (int) Math.pow(2, zoom);
            for (int x = 0; x < maxTile; x++) {
                for (int y = 0; y < maxTile; y++) {
                    mapService.getVideosForViewport(
                            tileToLat(y, zoom), tileToLat(y+1, zoom),
                            tileToLng(x, zoom), tileToLng(x+1, zoom),
                            zoom, null, null
                    );
                }
            }
        }
    }

    private double tileToLat(int y, int zoom) {
        double n = Math.PI - 2.0 * Math.PI * y / Math.pow(2, zoom);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    private double tileToLng(int x, int zoom) {
        return x / Math.pow(2, zoom) * 360.0 - 180.0;
    }
}
