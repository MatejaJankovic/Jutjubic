package rs.ftn.isa.jutjubicbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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
}
