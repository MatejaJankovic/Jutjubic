package rs.ftn.isa.jutjubicbackend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();


    @Column(nullable = false)
    private Long videoId;

    @Column(nullable = false)
    private String authorUsername;

    public Comment() {}

    public Comment(String text, Long videoId, String authorUsername) {
        this.text = text;
        this.videoId = videoId;
        this.authorUsername = authorUsername;
        this.createdAt = Instant.now();
    }

    // getters / setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
}
