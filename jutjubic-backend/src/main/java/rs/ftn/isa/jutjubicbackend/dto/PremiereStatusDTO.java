package rs.ftn.isa.jutjubicbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.ftn.isa.jutjubicbackend.model.PremiereStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiereStatusDTO {

    private Long videoId;
    private PremiereStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private Long startOffset;  // seconds from start - where the viewer should start playing
    private Integer videoDuration;  // total video duration in seconds
    private Boolean canSeek;  // whether seeking is allowed
    private Long viewerCount;  // number of current viewers (optional)
    private Long secondsUntilStart;  // seconds until premiere starts (for countdown)
}

