package rs.ftn.isa.jutjubicbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a transcoded variant of a video (e.g., 720p, 1080p)
 */
@Entity
@Table(name = "video_variants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(nullable = false, length = 20)
    private String profile; // 720p, 1080p, etc.

    @Column(name = "file_path", nullable = false)
    private String filePath; // Path to transcoded file

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

