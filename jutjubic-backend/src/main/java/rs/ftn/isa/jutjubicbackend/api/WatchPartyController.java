package rs.ftn.isa.jutjubicbackend.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ftn.isa.jutjubicbackend.dto.CreateWatchPartyRequest;
import rs.ftn.isa.jutjubicbackend.dto.SwitchVideoRequest;
import rs.ftn.isa.jutjubicbackend.dto.VideoPageResponse;
import rs.ftn.isa.jutjubicbackend.dto.WatchPartyDTO;
import rs.ftn.isa.jutjubicbackend.model.WatchParty;
import rs.ftn.isa.jutjubicbackend.service.VideoService;
import rs.ftn.isa.jutjubicbackend.service.WatchPartyService;

@Slf4j
@RestController
@RequestMapping("/api/watch-party")
@RequiredArgsConstructor
public class WatchPartyController {

    private final WatchPartyService watchPartyService;
    private final VideoService videoService;

    /**
     * Helper method to get current user ID from authentication context
     */
    private Long getCurrentUserId() {
        try {
            return videoService.getCurrentUser().getId();
        } catch (Exception e) {
            log.warn("Could not get authenticated user, using default ID 1");
            return 1L; // Fallback for testing without authentication
        }
    }

    @PostMapping("/create")
    public ResponseEntity<WatchPartyDTO> createWatchParty(@RequestBody CreateWatchPartyRequest request) {
        log.info("POST /api/watch-party/create - videoId: {}, isPublic: {}", request.getVideoId(), request.isPublic());

        Long creatorId = getCurrentUserId();

        WatchParty watchParty = watchPartyService.createRoom(
                creatorId,
                request.getVideoId(),
                request.isPublic()
        );

        WatchPartyDTO dto = WatchPartyDTO.fromEntity(watchParty);
        log.info("Watch party created successfully - ID: {}, invite code: {}", dto.getId(), dto.getInviteCode());

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<WatchPartyDTO> joinWatchParty(@PathVariable String inviteCode) {
        log.info("POST /api/watch-party/join/{}", inviteCode);

        Long userId = getCurrentUserId();

        try {
            WatchParty watchParty = watchPartyService.joinRoom(inviteCode, userId);
            WatchPartyDTO dto = WatchPartyDTO.fromEntity(watchParty);

            log.info("User {} joined watch party ID: {}", userId, dto.getId());
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            log.error("Failed to join watch party: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<WatchPartyDTO> getWatchParty(@PathVariable Long id) {
        log.info("GET /api/watch-party/{}", id);

        return watchPartyService.findById(id)
                .map(WatchPartyDTO::fromEntity)
                .map(dto -> {
                    log.info("Found watch party ID: {}", id);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    log.warn("Watch party not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping("/{roomId}/switch-video")
    public ResponseEntity<?> switchVideo(@PathVariable Long roomId, @RequestBody SwitchVideoRequest request) {
        log.info("POST /api/watch-party/{}/switch-video - newVideoId: {}", roomId, request.getVideoId());

        Long userId = getCurrentUserId();

        try {
            WatchParty watchParty = watchPartyService.switchVideo(roomId, request.getVideoId(), userId);
            WatchPartyDTO dto = WatchPartyDTO.fromEntity(watchParty);
            log.info("Video switched in room {} to video {} by user {}", roomId, request.getVideoId(), userId);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            log.error("Failed to switch video: {}", e.getMessage());
            if (e.getMessage().contains("owner")) {
                return ResponseEntity.status(403).body("Only the room owner can switch videos");
            }
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/recommended")
    public ResponseEntity<VideoPageResponse> getRecommendedVideos(
            @RequestParam(required = false) Long excludeVideoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /api/watch-party/recommended - excludeVideoId: {}, page: {}, size: {}", excludeVideoId, page, size);

        VideoPageResponse response = videoService.getRecommendedVideos(excludeVideoId, page, size);
        log.info("Returning {} recommended videos", response.getVideos().size());
        return ResponseEntity.ok(response);
    }
}

