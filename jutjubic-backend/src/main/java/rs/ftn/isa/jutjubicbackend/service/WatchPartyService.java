package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.isa.jutjubicbackend.model.WatchParty;
import rs.ftn.isa.jutjubicbackend.repository.WatchPartyRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchPartyService {

    private final WatchPartyRepository watchPartyRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public WatchParty createRoom(Long creatorId, Long videoId, boolean isPublic) {
        log.info("Creating watch party room for creator: {}, video: {}, public: {}", creatorId, videoId, isPublic);

        String inviteCode = generateUniqueInviteCode();

        WatchParty watchParty = WatchParty.builder()
                .creatorId(creatorId)
                .videoId(videoId)
                .isPublic(isPublic)
                .inviteCode(inviteCode)
                .createdAt(Instant.now())
                .participantIds(new HashSet<>())
                .build();

        // Add creator as first participant
        watchParty.getParticipantIds().add(creatorId);

        WatchParty saved = watchPartyRepository.save(watchParty);
        log.info("Watch party created with ID: {}, invite code: {}", saved.getId(), saved.getInviteCode());

        return saved;
    }

    @Transactional
    public WatchParty joinRoom(String inviteCode, Long userId) {
        log.info("User {} attempting to join watch party with invite code: {}", userId, inviteCode);

        WatchParty watchParty = watchPartyRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> {
                    log.error("Watch party not found with invite code: {}", inviteCode);
                    return new RuntimeException("Watch party not found");
                });

        // Add user to participants if not already present
        boolean isNewParticipant = watchParty.getParticipantIds().add(userId);

        if (isNewParticipant) {
            log.info("User {} joined watch party ID: {}", userId, watchParty.getId());
            WatchParty saved = watchPartyRepository.save(watchParty);

            // Broadcast USER_JOINED event to all participants in the room
            broadcastUserJoined(saved, userId);

            return saved;
        } else {
            log.info("User {} already in watch party ID: {}", userId, watchParty.getId());
            return watchParty;
        }
    }

    /**
     * Broadcasts USER_JOINED event to all participants in the watch party room
     */
    private void broadcastUserJoined(WatchParty watchParty, Long userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "USER_JOINED");
        event.put("roomId", watchParty.getId());
        event.put("videoId", watchParty.getVideoId());
        event.put("userId", userId);
        event.put("participantCount", watchParty.getParticipantIds().size());
        event.put("timestamp", Instant.now().toString());

        String destination = "/topic/watch-party/" + watchParty.getId();
        log.info("Broadcasting USER_JOINED event to {}: {}", destination, event);

        messagingTemplate.convertAndSend(destination, event);
    }

    public Optional<WatchParty> findById(Long id) {
        log.debug("Finding watch party by ID: {}", id);
        return watchPartyRepository.findById(id);
    }

    /**
     * Switch the current video in a watch party room.
     * Only the owner (creator) can switch videos.
     */
    @Transactional
    public WatchParty switchVideo(Long roomId, Long newVideoId, Long userId) {
        log.info("User {} attempting to switch video in room {} to video {}", userId, roomId, newVideoId);

        WatchParty watchParty = watchPartyRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.error("Watch party not found with ID: {}", roomId);
                    return new RuntimeException("Watch party not found");
                });

        // Only the creator (owner) can switch videos
        if (!watchParty.getCreatorId().equals(userId)) {
            log.warn("User {} is not the owner of room {}. Owner is {}", userId, roomId, watchParty.getCreatorId());
            throw new RuntimeException("Only the room owner can switch videos");
        }

        // Update the current video
        watchParty.setCurrentVideoId(newVideoId);
        WatchParty saved = watchPartyRepository.save(watchParty);

        // Broadcast VIDEO_SWITCHED event to all participants
        broadcastVideoSwitched(saved, newVideoId, userId);

        log.info("Video switched in room {} to video {} by owner {}", roomId, newVideoId, userId);
        return saved;
    }

    /**
     * Broadcasts VIDEO_SWITCHED event to all participants in the watch party room
     */
    private void broadcastVideoSwitched(WatchParty watchParty, Long newVideoId, Long triggeredBy) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "VIDEO_SWITCHED");
        event.put("roomId", watchParty.getId());
        event.put("videoId", newVideoId);
        event.put("previousVideoId", watchParty.getVideoId());
        event.put("triggeredBy", triggeredBy);
        event.put("timestamp", Instant.now().toString());

        String destination = "/topic/watch-party/" + watchParty.getId();
        log.info("Broadcasting VIDEO_SWITCHED event to {}: {}", destination, event);

        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * Generates a unique invite code using UUID
     */
    private String generateUniqueInviteCode() {
        String inviteCode;
        do {
            inviteCode = UUID.randomUUID().toString();
        } while (watchPartyRepository.existsByInviteCode(inviteCode));

        return inviteCode;
    }
}

