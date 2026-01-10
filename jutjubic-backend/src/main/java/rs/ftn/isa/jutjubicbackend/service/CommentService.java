// java
package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import rs.ftn.isa.jutjubicbackend.dto.CommentDto;
import rs.ftn.isa.jutjubicbackend.model.Comment;
import rs.ftn.isa.jutjubicbackend.model.Video;
import rs.ftn.isa.jutjubicbackend.repository.CommentRepository;
import rs.ftn.isa.jutjubicbackend.repository.VideoRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;

    @Cacheable(cacheNames = "comments", key = "#videoId + ':' + #page + ':' + #size")
    public Page<CommentDto> getComments(Long videoId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Comment> comments = commentRepository.findByVideoIdOrderByCreatedAtDesc(videoId, pageable);
        return comments.map(this::toDto);
    }

    @Transactional
    @CacheEvict(cacheNames = "comments", allEntries = true)
    public CommentDto createComment(Long videoId, String text, String authorUsername) {
        Comment comment = new Comment(text, videoId, authorUsername);
        comment.setCreatedAt(Instant.now());
        Comment saved = commentRepository.save(comment);

        // Increment video's comment count if video exists
        Optional<Video> vOpt = videoRepository.findById(videoId);
        vOpt.ifPresent(video -> {
            Long current = video.getCommentCount() == null ? 0L : video.getCommentCount();
            video.setCommentCount(current + 1);
            videoRepository.save(video);
        });

        return toDto(saved);
    }

    private CommentDto toDto(Comment c) {
        return new CommentDto(c.getId(), c.getText(), c.getAuthorUsername(), c.getCreatedAt());
    }
}
