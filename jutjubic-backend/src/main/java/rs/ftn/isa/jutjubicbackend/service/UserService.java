// java
// File: jutjubic-backend/src/main/java/rs/ftn/isa/jutjubicbackend/service/UserService.java
package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.ftn.isa.jutjubicbackend.dto.UserProfileDto;
import rs.ftn.isa.jutjubicbackend.model.User;
import rs.ftn.isa.jutjubicbackend.repository.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Optional<UserProfileDto> getPublicProfile(String username) {
        return userRepository.findByUsername(username)
                .map(UserProfileDto::fromEntity);
    }
}
