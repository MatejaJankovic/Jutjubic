package rs.ftn.isa.jutjubicbackend.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ftn.isa.jutjubicbackend.service.ThumbnailCompressionService;

@RestController
@RequestMapping("/api/admin/maintenance")
@RequiredArgsConstructor
public class AdminMaintenanceController {

    private final ThumbnailCompressionService thumbnailCompressionService;

    @PostMapping("/compress-thumbnails")
    public ResponseEntity<String> compressThumbnails() {
        thumbnailCompressionService.compressOldThumbnails();
        return ResponseEntity.ok("Compression job executed successfully");
    }

    @PostMapping("/compress-all-thumbnails")
    public ResponseEntity<String> compressAllThumbnails() {
        thumbnailCompressionService.compressAllUncompressedThumbnails();
        return ResponseEntity.ok("Compression job for ALL thumbnails executed successfully");
    }
}

