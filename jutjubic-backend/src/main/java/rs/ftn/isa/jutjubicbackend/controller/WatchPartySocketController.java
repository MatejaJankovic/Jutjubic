package rs.ftn.isa.jutjubicbackend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import rs.ftn.isa.jutjubicbackend.dto.WatchPartyPlayEvent;
import rs.ftn.isa.jutjubicbackend.service.WatchPartyService;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WatchPartySocketController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final WatchPartyService watchPartyService;

    @MessageMapping("/watch-party/{roomId}/play")
    public void playVideo(@DestinationVariable Long roomId, WatchPartyPlayEvent event) {
        log.info("Watch party play event received - roomId: {}, videoId: {}, triggeredBy: {}, triggeredAt: {}",
                roomId, event.getVideoId(), event.getTriggeredBy(), event.getTriggeredAt());

        // Broadcast the play event to all subscribers in the room
        String destination = "/topic/watch-party/" + roomId;
        simpMessagingTemplate.convertAndSend(destination, event);

        log.info("Play event broadcasted to {}", destination);
    }

    @MessageMapping("/watch-party/{roomId}/start")
    public void startVideo(@DestinationVariable Long roomId, @Payload Map<String, Object> event) {
        log.info("Watch party start event received - roomId: {}, event: {}", roomId, event);

        // Broadcast the start event to all subscribers in the room
        String destination = "/topic/watch-party/" + roomId;
        simpMessagingTemplate.convertAndSend(destination, event);

        log.info("Start event broadcasted to {}", destination);
    }

    @MessageMapping("/watch-party/{roomId}/switch-video")
    public void switchVideo(@DestinationVariable Long roomId, @Payload Map<String, Object> event) {
        log.info("Watch party switch-video event received - roomId: {}, event: {}", roomId, event);

        Long newVideoId = ((Number) event.get("videoId")).longValue();
        Long userId = ((Number) event.get("userId")).longValue();

        try {
            // Validate and persist the switch (also broadcasts the event)
            watchPartyService.switchVideo(roomId, newVideoId, userId);
            log.info("Video switched via WebSocket in room {} to video {} by user {}", roomId, newVideoId, userId);
        } catch (RuntimeException e) {
            log.error("Failed to switch video via WebSocket: {}", e.getMessage());
            // Could send an error event back to the user here if needed
        }
    }

    /**
     * Handle PLAYBACK_PLAY event - owner plays the video
     */
    @MessageMapping("/watch-party/{roomId}/playback-play")
    public void handlePlaybackPlay(@DestinationVariable Long roomId, @Payload Map<String, Object> event) {
        log.info("Watch party PLAYBACK_PLAY event received - roomId: {}, event: {}", roomId, event);

        // Add event type if not present
        event.put("type", "PLAYBACK_PLAY");

        // Broadcast to all participants
        String destination = "/topic/watch-party/" + roomId;
        simpMessagingTemplate.convertAndSend(destination, event);

        log.info("PLAYBACK_PLAY event broadcasted to {}", destination);
    }

    /**
     * Handle PLAYBACK_PAUSE event - owner pauses the video
     */
    @MessageMapping("/watch-party/{roomId}/playback-pause")
    public void handlePlaybackPause(@DestinationVariable Long roomId, @Payload Map<String, Object> event) {
        log.info("Watch party PLAYBACK_PAUSE event received - roomId: {}, event: {}", roomId, event);

        // Add event type if not present
        event.put("type", "PLAYBACK_PAUSE");

        // Broadcast to all participants
        String destination = "/topic/watch-party/" + roomId;
        simpMessagingTemplate.convertAndSend(destination, event);

        log.info("PLAYBACK_PAUSE event broadcasted to {}", destination);
    }

    /**
     * Handle PLAYBACK_SEEK event - owner seeks to a position
     */
    @MessageMapping("/watch-party/{roomId}/playback-seek")
    public void handlePlaybackSeek(@DestinationVariable Long roomId, @Payload Map<String, Object> event) {
        log.info("Watch party PLAYBACK_SEEK event received - roomId: {}, event: {}", roomId, event);

        // Add event type if not present
        event.put("type", "PLAYBACK_SEEK");

        // Broadcast to all participants
        String destination = "/topic/watch-party/" + roomId;
        simpMessagingTemplate.convertAndSend(destination, event);

        log.info("PLAYBACK_SEEK event broadcasted to {}", destination);
    }

    /**
     * Handle PLAYBACK_SYNC request - guest requests current playback state
     */
    @MessageMapping("/watch-party/{roomId}/playback-sync-request")
    public void handlePlaybackSyncRequest(@DestinationVariable Long roomId, @Payload Map<String, Object> event) {
        log.info("Watch party PLAYBACK_SYNC_REQUEST event received - roomId: {}, event: {}", roomId, event);

        // Add event type
        event.put("type", "PLAYBACK_SYNC_REQUEST");

        // Broadcast to all participants (owner will respond with current state)
        String destination = "/topic/watch-party/" + roomId;
        simpMessagingTemplate.convertAndSend(destination, event);

        log.info("PLAYBACK_SYNC_REQUEST event broadcasted to {}", destination);
    }

    /**
     * Handle PLAYBACK_SYNC response - owner sends current playback state
     */
    @MessageMapping("/watch-party/{roomId}/playback-sync-response")
    public void handlePlaybackSyncResponse(@DestinationVariable Long roomId, @Payload Map<String, Object> event) {
        log.info("Watch party PLAYBACK_SYNC_RESPONSE event received - roomId: {}, event: {}", roomId, event);

        // Add event type
        event.put("type", "PLAYBACK_SYNC_RESPONSE");

        // Broadcast to all participants
        String destination = "/topic/watch-party/" + roomId;
        simpMessagingTemplate.convertAndSend(destination, event);

        log.info("PLAYBACK_SYNC_RESPONSE event broadcasted to {}", destination);
    }
}

