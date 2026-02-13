package rs.ftn.isa.jutjubicbackend.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ftn.isa.jutjubicbackend.dto.PopularVideosResponseDTO;
import rs.ftn.isa.jutjubicbackend.service.ETLService;

@RestController
@RequestMapping("/api/etl")
@RequiredArgsConstructor
public class ETLController {

    private final ETLService etlService;

    /**
     * Get the latest popular videos from the most recent ETL pipeline run
     */
    @GetMapping("/popular-videos")
    public ResponseEntity<PopularVideosResponseDTO> getPopularVideos() {
        return etlService.getLatestPopularVideos()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Manually trigger ETL pipeline (for testing/admin purposes)
     */
    @PostMapping("/run-pipeline")
    public ResponseEntity<String> runPipeline() {
        try {
            etlService.runETLPipeline();
            return ResponseEntity.ok("ETL Pipeline executed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("ETL Pipeline failed: " + e.getMessage());
        }
    }
}

