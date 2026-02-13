package rs.ftn.isa.jutjubicbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "popular_videos", indexes = {
    @Index(name = "idx_pipeline_run_at", columnList = "pipeline_run_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pipeline_run_at", nullable = false)
    private LocalDateTime pipelineRunAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "first_video_id")
    private Video firstVideo;

    @Column(name = "first_video_score")
    private Double firstVideoScore;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "second_video_id")
    private Video secondVideo;

    @Column(name = "second_video_score")
    private Double secondVideoScore;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "third_video_id")
    private Video thirdVideo;

    @Column(name = "third_video_score")
    private Double thirdVideoScore;

    @PrePersist
    protected void onCreate() {
        if (pipelineRunAt == null) {
            pipelineRunAt = LocalDateTime.now();
        }
    }
}

