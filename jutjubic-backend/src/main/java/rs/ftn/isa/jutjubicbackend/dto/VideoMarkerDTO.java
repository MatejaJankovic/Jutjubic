package rs.ftn.isa.jutjubicbackend.dto;

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
public class VideoMarkerDTO {
    private Long id;
    private String title;
    private Double latitude;
    private Double longitude;
    private String thumbnailUrl;
    private Long viewCount;
    private LocalDateTime createdAt;
    private String username;

    public static VideoMarkerDTO fromEntity(Video video) {
        return VideoMarkerDTO.builder()
                .id(video.getId())
                .title(video.getTitle())
                .latitude(video.getLatitude())
                .longitude(video.getLongitude())
                .thumbnailUrl(video.getThumbnailUrl())
                .viewCount(video.getViewCount())
                .createdAt(video.getCreatedAt())
                .username(video.getUser().getUsername())
                .build();
    }
}

