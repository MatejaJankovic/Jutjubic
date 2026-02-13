package rs.ftn.isa.jutjubicbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.ftn.isa.jutjubicbackend.model.PopularVideo;

import java.util.Optional;

@Repository
public interface PopularVideoRepository extends JpaRepository<PopularVideo, Long> {

    @Query("SELECT pv FROM PopularVideo pv ORDER BY pv.pipelineRunAt DESC LIMIT 1")
    Optional<PopularVideo> findLatest();
}

