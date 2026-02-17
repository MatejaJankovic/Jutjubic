package rs.ftn.isa.mqconsumer;
import lombok.Data;

@Data
public class UploadEvent {
    private String title;
    private String author;
    private long size;
    private long timestamp;
}
