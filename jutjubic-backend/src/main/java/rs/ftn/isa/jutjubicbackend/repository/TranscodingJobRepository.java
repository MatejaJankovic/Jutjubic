package rs.ftn.isa.jutjubicbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.ftn.isa.jutjubicbackend.model.TranscodingJob;

import java.util.Optional;

/**
 * Repository for managing transcoding job state (idempotency)
 */
@Repository
public interface TranscodingJobRepository extends JpaRepository<TranscodingJob, Long> {

    /**
     * Find transcoding job by video ID
     * @param videoId Video entity ID
     * @return Optional containing the transcoding job if exists
     */
    Optional<TranscodingJob> findByVideoId(Long videoId);
}

