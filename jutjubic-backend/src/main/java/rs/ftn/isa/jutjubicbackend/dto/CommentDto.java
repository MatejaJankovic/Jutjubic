package rs.ftn.isa.jutjubicbackend.dto;

import java.time.Instant;

public class CommentDto {
    private Long id;
    private String text;
    private String authorUsername;
    private Instant createdAt;

    public CommentDto() {}

    public CommentDto(Long id, String text, String authorUsername, Instant createdAt) {
        this.id = id;
        this.text = text;
        this.authorUsername = authorUsername;
        this.createdAt = createdAt;
    }

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
