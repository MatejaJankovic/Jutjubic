package rs.ftn.isa.jutjubicbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.ftn.isa.jutjubicbackend.model.PremiereStatus;
import rs.ftn.isa.jutjubicbackend.model.Video;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDTO {

    private Long id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long userId;
    private String username;
    private String userFirstName;
    private String userLastName;
    private LocalDateTime createdAt;
    private boolean likedByCurrentUser;
    private Double latitude;
    private Double longitude;

    // Premiere fields - only scheduledAt is stored, status is calculated dynamically
    private LocalDateTime premiereScheduledAt;
    private PremiereStatus premiereStatus; // Calculated dynamically, not from DB

    @JsonProperty("isLikedByCurrentUser")
    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    @JsonProperty("isLikedByCurrentUser")
    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }


    public static VideoDTO fromEntity(Video video) {
        // Calculate premiere status dynamically based on current time
        PremiereStatus calculatedStatus = calculatePremiereStatus(video);

        return VideoDTO.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .videoUrl(video.getVideoUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .durationSeconds(video.getDurationSeconds())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .userId(video.getUser().getId())
                .username(video.getUser().getUsername())
                .userFirstName(video.getUser().getFirstName())
                .userLastName(video.getUser().getLastName())
                .createdAt(video.getCreatedAt())
                .latitude(video.getLatitude())
                .longitude(video.getLongitude())
                .premiereScheduledAt(video.getPremiereScheduledAt())
                .premiereStatus(calculatedStatus)
                .build();
    }

    /**
     * Calculate premiere status dynamically based on current time.
     * - If premiereScheduledAt is null -> null (not a premiere)
     * - If premiereScheduledAt is in the future -> SCHEDULED
     * - If current time is between premiereScheduledAt and premiereScheduledAt + duration -> LIVE
     * - If current time is after premiereScheduledAt + duration -> ENDED (video becomes regular)
     */
    private static PremiereStatus calculatePremiereStatus(Video video) {
        if (video.getPremiereScheduledAt() == null) {
            return null; // Not a premiere video
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledAt = video.getPremiereScheduledAt();
        Integer durationSeconds = video.getDurationSeconds();

        // If scheduled time is in the future -> SCHEDULED
        if (now.isBefore(scheduledAt)) {
            return PremiereStatus.SCHEDULED;
        }

        // If we don't have duration, treat as LIVE indefinitely (fallback)
        if (durationSeconds == null || durationSeconds <= 0) {
            return PremiereStatus.LIVE;
        }

        LocalDateTime endTime = scheduledAt.plusSeconds(durationSeconds);

        // If current time is between start and end -> LIVE
        if (now.isBefore(endTime)) {
            return PremiereStatus.LIVE;
        }

        // If current time is after end -> ENDED (premiere is over)
        return PremiereStatus.ENDED;
    }
}

