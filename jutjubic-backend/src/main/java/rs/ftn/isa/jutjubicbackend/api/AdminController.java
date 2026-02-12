package rs.ftn.isa.jutjubicbackend.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ftn.isa.jutjubicbackend.service.TestDataGenerator;
import rs.ftn.isa.jutjubicbackend.service.VideoService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative endpoints for testing and data management")
public class AdminController {

    private final TestDataGenerator testDataGenerator;
    private final VideoService videoService;

    @PostMapping("/generate-test-videos")
    @Operation(summary = "Generate test videos",
               description = "Generates test videos with random coordinates across Europe for tile system testing")
    public ResponseEntity<MessageResponse> generateTestVideos(
            @RequestParam(defaultValue = "5000") int count) {

        testDataGenerator.generateTestVideos(count);

        return ResponseEntity.ok(
                MessageResponse.builder()
                        .message("Successfully generated " + count + " test videos with random European coordinates")
                        .count(count)
                        .build()
        );
    }

    @DeleteMapping("/delete-test-videos")
    @Operation(summary = "Delete test videos",
               description = "Deletes all test videos created by test user")
    public ResponseEntity<MessageResponse> deleteTestVideos() {
        testDataGenerator.deleteAllTestVideos();

        return ResponseEntity.ok(
                MessageResponse.builder()
                        .message("Successfully deleted all test videos")
                        .build()
        );
    }

    @PostMapping("/update-video-durations")
    @Operation(summary = "Update video durations",
               description = "Updates duration_seconds for all existing videos that have null duration")
    public ResponseEntity<MessageResponse> updateVideoDurations() {
        int updated = videoService.updateAllVideoDurations();

        return ResponseEntity.ok(
                MessageResponse.builder()
                        .message("Successfully updated durations for " + updated + " videos")
                        .count(updated)
                        .build()
        );
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class MessageResponse {
        private String message;
        private Integer count;
    }
}

