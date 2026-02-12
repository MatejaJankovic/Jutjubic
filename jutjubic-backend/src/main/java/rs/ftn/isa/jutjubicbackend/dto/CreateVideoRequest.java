package rs.ftn.isa.jutjubicbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateVideoRequest {

    @NotBlank
    private String title;

    private String description;

    private List<String> tags;

    private String location;

    private Double latitude;

    private Double longitude;

    // Premiere scheduling - only premiereScheduledAt needed
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime premiereScheduledAt;
}
