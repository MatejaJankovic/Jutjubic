package rs.ftn.isa.jutjubicbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.ftn.isa.jutjubicbackend.model.WatchParty;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchPartyDTO {

    private Long id;
    private Long creatorId;
    private Long videoId;
    private Long currentVideoId;
    private Instant createdAt;
    private boolean isPublic;
    private String inviteCode;
    private Set<Long> participantIds;

    public static WatchPartyDTO fromEntity(WatchParty watchParty) {
        return WatchPartyDTO.builder()
                .id(watchParty.getId())
                .creatorId(watchParty.getCreatorId())
                .videoId(watchParty.getVideoId())
                .currentVideoId(watchParty.getCurrentVideoId() != null ? watchParty.getCurrentVideoId() : watchParty.getVideoId())
                .createdAt(watchParty.getCreatedAt())
                .isPublic(watchParty.isPublic())
                .inviteCode(watchParty.getInviteCode())
                .participantIds(watchParty.getParticipantIds())
                .build();
    }
}

