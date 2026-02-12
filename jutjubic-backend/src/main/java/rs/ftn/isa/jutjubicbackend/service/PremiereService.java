package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.isa.jutjubicbackend.dto.PremiereStatusDTO;
import rs.ftn.isa.jutjubicbackend.model.PremiereStatus;
import rs.ftn.isa.jutjubicbackend.model.Video;
import rs.ftn.isa.jutjubicbackend.repository.VideoRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PremiereService {

    private final VideoRepository videoRepository;

    // Track unique viewers by IP address for each video
    private final ConcurrentHashMap<Long, Set<String>> liveViewers = new ConcurrentHashMap<>();

    /**
     * Get the current premiere status for a video
     */
    public Optional<PremiereStatusDTO> getPremiereStatus(Long videoId) {
        return videoRepository.findById(videoId).map(this::buildPremiereStatus);
    }

    /**
     * Build premiere status DTO from video entity.
     * Status is calculated dynamically based on current time, scheduledAt and duration.
     */
    private PremiereStatusDTO buildPremiereStatus(Video video) {
        LocalDateTime now = LocalDateTime.now();

        // Calculate status dynamically
        PremiereStatus status = calculateDynamicStatus(video, now);

        Long startOffset = 0L;
        Long secondsUntilStart = 0L;
        Boolean canSeek = true;

        if (video.getPremiereScheduledAt() != null) {
            LocalDateTime scheduledAt = video.getPremiereScheduledAt();

            switch (status) {
                case SCHEDULED:
                    // Calculate countdown
                    secondsUntilStart = ChronoUnit.SECONDS.between(now, scheduledAt);
                    if (secondsUntilStart < 0) secondsUntilStart = 0L;
                    canSeek = false;
                    break;

                case LIVE:
                    // Calculate current offset from scheduled start time
                    startOffset = ChronoUnit.SECONDS.between(scheduledAt, now);
                    if (startOffset < 0) startOffset = 0L;
                    canSeek = false;
                    break;

                case ENDED:
                    // Normal playback - premiere is over
                    canSeek = true;
                    startOffset = 0L;
                    break;
            }
        }

        return PremiereStatusDTO.builder()
                .videoId(video.getId())
                .status(status)
                .scheduledAt(video.getPremiereScheduledAt())
                .startedAt(video.getPremiereScheduledAt()) // Use scheduledAt as startedAt
                .startOffset(startOffset)
                .videoDuration(video.getDurationSeconds())
                .canSeek(canSeek)
                .viewerCount(getViewerCount(video.getId()))
                .secondsUntilStart(secondsUntilStart)
                .build();
    }

    /**
     * Calculate premiere status dynamically based on current time.
     * No database writes needed - pure calculation.
     */
    private PremiereStatus calculateDynamicStatus(Video video, LocalDateTime now) {
        if (video.getPremiereScheduledAt() == null) {
            return PremiereStatus.ENDED; // Not a premiere or no schedule = regular video
        }

        LocalDateTime scheduledAt = video.getPremiereScheduledAt();
        Integer durationSeconds = video.getDurationSeconds();

        // If scheduled time is in the future -> SCHEDULED
        if (now.isBefore(scheduledAt)) {
            return PremiereStatus.SCHEDULED;
        }

        // If no duration, can't determine end time - treat as LIVE
        if (durationSeconds == null || durationSeconds <= 0) {
            return PremiereStatus.LIVE;
        }

        LocalDateTime endTime = scheduledAt.plusSeconds(durationSeconds);

        // If current time is between start and end -> LIVE
        if (now.isBefore(endTime)) {
            return PremiereStatus.LIVE;
        }

        // If current time is after end -> ENDED
        return PremiereStatus.ENDED;
    }

    /**
     * Check if a video can be accessed (for premieres that haven't started yet)
     */
    public boolean canAccessVideo(Long videoId) {
        return videoRepository.findById(videoId)
                .map(video -> {
                    if (video.getPremiereScheduledAt() == null) {
                        return true; // Regular video
                    }

                    // Use dynamically calculated status
                    PremiereStatus status = calculateDynamicStatus(video, LocalDateTime.now());

                    return switch (status) {
                        case SCHEDULED -> false; // Not yet available
                        case LIVE, ENDED -> true; // Available
                    };
                })
                .orElse(false);
    }

    /**
     * Register a viewer for counting purposes using their IP address
     */
    public void registerViewer(Long videoId, String ipAddress) {
        liveViewers.computeIfAbsent(videoId, k -> ConcurrentHashMap.newKeySet()).add(ipAddress);
    }

    /**
     * Unregister a viewer using their IP address
     */
    public void unregisterViewer(Long videoId, String ipAddress) {
        Set<String> viewers = liveViewers.get(videoId);
        if (viewers != null) {
            viewers.remove(ipAddress);
            if (viewers.isEmpty()) {
                liveViewers.remove(videoId);
            }
        }
    }

    /**
     * Get current viewer count
     */
    public Long getViewerCount(Long videoId) {
        Set<String> viewers = liveViewers.get(videoId);
        return viewers != null ? (long) viewers.size() : 0L;
    }
}

