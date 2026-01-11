// java
// File: jutjubic-backend/src/main/java/rs/ftn/isa/jutjubicbackend/api/UserController.java
package rs.ftn.isa.jutjubicbackend.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ftn.isa.jutjubicbackend.dto.UserProfileDto;
import rs.ftn.isa.jutjubicbackend.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileDto> getPublicProfile(@PathVariable String username) {
        return userService.getPublicProfile(username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
