package rs.ftn.isa.jutjubicbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.ftn.isa.jutjubicbackend.model.VideoVariant;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing video transcoding variants
 */
@Repository
public interface VideoVariantRepository extends JpaRepository<VideoVariant, Long> {

    /**
     * Find all variants for a specific video
     */
    List<VideoVariant> findByVideoId(Long videoId);

    /**
     * Find a specific variant by video ID and profile
     */
    Optional<VideoVariant> findByVideoIdAndProfile(Long videoId, String profile);
}

