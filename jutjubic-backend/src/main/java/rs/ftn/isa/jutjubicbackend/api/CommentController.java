package rs.ftn.isa.jutjubicbackend.api;

import  rs.ftn.isa.jutjubicbackend.dto.CommentDto;
import  rs.ftn.isa.jutjubicbackend.dto.CreateCommentRequest;
import  rs.ftn.isa.jutjubicbackend.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/videos/{videoId}/comments")
public class CommentController {

    private final CommentService service;

    @Autowired
    public CommentController(CommentService service) {
        this.service = service;
    }

    // public endpoint - paginated newest first
    @GetMapping
    public ResponseEntity<Page<CommentDto>> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<CommentDto> comments = service.getComments(videoId, page, size);
        return ResponseEntity.ok(comments);
    }

    // authenticated only: require Principal (if null -> 401)
    @PostMapping
    public ResponseEntity<?> postComment(
            @PathVariable Long videoId,
            @RequestBody CreateCommentRequest req,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (req.getText() == null || req.getText().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Text is required");
        }
        CommentDto created = service.createComment(videoId, req.getText().trim(), principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
