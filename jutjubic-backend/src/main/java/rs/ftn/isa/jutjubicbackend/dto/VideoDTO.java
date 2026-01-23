package rs.ftn.isa.jutjubicbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @JsonProperty("isLikedByCurrentUser")
    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    @JsonProperty("isLikedByCurrentUser")
    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }

    public static VideoDTO fromEntity(Video video) {
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
                .build();
    }
}

