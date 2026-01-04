package rs.ftn.isa.jutjubicbackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ftn.isa.jutjubicbackend.model.Video;

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
}

