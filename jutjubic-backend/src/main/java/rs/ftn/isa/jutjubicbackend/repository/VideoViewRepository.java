package rs.ftn.isa.jutjubicbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ftn.isa.jutjubicbackend.model.VideoView;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VideoViewRepository extends JpaRepository<VideoView, Long> {

    @Query("SELECT vv FROM VideoView vv WHERE vv.viewedAt >= :startDate")
    List<VideoView> findViewsSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT vv FROM VideoView vv WHERE vv.viewedAt >= :startDate AND vv.viewedAt < :endDate")
    List<VideoView> findViewsBetween(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
}

