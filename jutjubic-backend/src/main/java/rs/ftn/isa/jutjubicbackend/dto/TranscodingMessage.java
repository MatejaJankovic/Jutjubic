package rs.ftn.isa.jutjubicbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscodingMessage {

    @JsonProperty("videoId")
    private Long videoId;

    @JsonProperty("inputPath")
    private String inputPath;

    @JsonProperty("requestedProfiles")
    private List<String> requestedProfiles;

    @JsonProperty("createdAt")
    private Instant createdAt;
}

