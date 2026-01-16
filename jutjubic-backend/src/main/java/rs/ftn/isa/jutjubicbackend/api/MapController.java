package rs.ftn.isa.jutjubicbackend.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ftn.isa.jutjubicbackend.dto.VideoMarkerDTO;
import rs.ftn.isa.jutjubicbackend.service.MapService;

import java.util.List;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
@Tag(name = "Map", description = "Map and geolocation endpoints")
public class MapController {

    private final MapService mapService;

    @GetMapping("/videos")
    @Operation(summary = "Get videos for map viewport",
               description = "Returns video markers within the specified map bounds using tile-based approach")
    public ResponseEntity<List<VideoMarkerDTO>> getVideosForViewport(
            @RequestParam Double north,
            @RequestParam Double south,
            @RequestParam Double east,
            @RequestParam Double west,
            @RequestParam(required = false) Integer zoom) {

        List<VideoMarkerDTO> markers = mapService.getVideosForViewport(north, south, east, west, zoom);
        return ResponseEntity.ok(markers);
    }
}

