package rs.ftn.isa.jutjubicbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWatchPartyRequest {

    private Long videoId;
    private boolean isPublic;
}

