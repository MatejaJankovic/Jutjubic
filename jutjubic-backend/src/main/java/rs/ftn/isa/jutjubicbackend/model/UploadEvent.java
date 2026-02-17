package rs.ftn.isa.jutjubicbackend.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadEvent {

    private String title;
    private String author;
    private long size;
    private long timestamp;
}
