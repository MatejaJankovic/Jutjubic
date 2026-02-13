package rs.ftn.isa.jutjubicbackend.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ftn.isa.jutjubicbackend.dto.PremiereStatusDTO;
import rs.ftn.isa.jutjubicbackend.service.PremiereService;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class PremiereController {

    private final PremiereService premiereService;

    /**
     * Get the current premiere status for a video
     * This endpoint is used for polling during premiere playback
     */
    @GetMapping("/{id}/premiere-status")
    public ResponseEntity<PremiereStatusDTO> getPremiereStatus(@PathVariable Long id) {
        return premiereService.getPremiereStatus(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Check if a video can be accessed (for premiere access control)
     */
    @GetMapping("/{id}/can-access")
    public ResponseEntity<Boolean> canAccessVideo(@PathVariable Long id) {
        return ResponseEntity.ok(premiereService.canAccessVideo(id));
    }

    /**
     * Register a viewer for the premiere (for viewer count tracking)
     */
    @PostMapping("/{id}/premiere-join")
    public ResponseEntity<Void> joinPremiere(@PathVariable Long id, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        premiereService.registerViewer(id, ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Unregister a viewer from the premiere
     */
    @PostMapping("/{id}/premiere-leave")
    public ResponseEntity<Void> leavePremiere(@PathVariable Long id, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        premiereService.unregisterViewer(id, ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}

