package rs.ftn.isa.jutjubicbackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.isa.jutjubicbackend.model.Video;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    Page<Video> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Video> findAllByOrderByCreatedAtDesc();

    Page<Video> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE LOWER(v.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY v.createdAt DESC")
    Page<Video> searchByTitle(@Param("query") String query, Pageable pageable);

    @Query("SELECT v FROM Video v ORDER BY v.viewCount DESC")
    Page<Video> findTrending(Pageable pageable);

    @Transactional
    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :id")
    int incrementViewCountById(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("UPDATE Video v SET v.likeCount = v.likeCount + 1 WHERE v.id = :id")
    int incrementLikeCountById(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("UPDATE Video v SET v.likeCount = v.likeCount - 1 WHERE v.id = :id AND v.likeCount > 0")
    int decrementLikeCountById(@Param("id") Long id);

    @Query("SELECT v FROM Video v WHERE v.latitude IS NOT NULL AND v.longitude IS NOT NULL " +
            "AND v.latitude BETWEEN :south AND :north " +
            "AND v.longitude BETWEEN :west AND :east " +
            "ORDER BY v.createdAt DESC")
    List<Video> findVideosInBounds(@Param("north") Double north,
                                    @Param("south") Double south,
                                    @Param("east") Double east,
                                    @Param("west") Double west);

    @Query("""
    SELECT v FROM Video v
    WHERE v.latitude IS NOT NULL
      AND v.longitude IS NOT NULL
      AND v.latitude BETWEEN :south AND :north
      AND v.longitude BETWEEN :west AND :east
      AND v.createdAt >= :startDate
      AND v.createdAt <= :endDate
    ORDER BY v.createdAt DESC
""")
    List<Video> findVideosInBoundsAndCreatedBetween(
            @Param("north") Double north,
            @Param("south") Double south,
            @Param("east") Double east,
            @Param("west") Double west,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

}

