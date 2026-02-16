package rs.ftn.isa.jutjubicbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "watch_parties")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "current_video_id")
    private Long currentVideoId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "invite_code", nullable = false, unique = true, length = 36)
    private String inviteCode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "watch_party_participants", joinColumns = @JoinColumn(name = "watch_party_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<Long> participantIds = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        // Default currentVideoId to videoId on creation
        if (currentVideoId == null) {
            currentVideoId = videoId;
        }
    }
}

