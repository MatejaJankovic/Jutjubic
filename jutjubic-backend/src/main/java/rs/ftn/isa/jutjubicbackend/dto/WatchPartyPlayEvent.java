package rs.ftn.isa.jutjubicbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchPartyPlayEvent {

    private Long roomId;
    private Long videoId;
    private Long triggeredBy;
    private Instant triggeredAt;
}
