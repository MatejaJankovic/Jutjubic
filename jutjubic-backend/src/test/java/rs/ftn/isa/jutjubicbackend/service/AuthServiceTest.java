package rs.ftn.isa.jutjubicbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.ftn.isa.jutjubicbackend.dto.LoginRequest;
import rs.ftn.isa.jutjubicbackend.dto.RegisterRequest;
import rs.ftn.isa.jutjubicbackend.exception.BadRequestException;
import rs.ftn.isa.jutjubicbackend.model.ActiveUsersMetrics;
import rs.ftn.isa.jutjubicbackend.repository.UserRepository;
import rs.ftn.isa.jutjubicbackend.security.JwtTokenProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;
    @Mock private ActiveUsersMetrics activeUsersMetrics;

    @InjectMocks
    private AuthService authService;

    // Test 1: Registracija odbija kad se lozinke ne poklapaju (nema DB poziva)
    @Test
    void register_throwsWhenPasswordsDontMatch() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com", "username123", "password123", "different456",
                "Ime", "Prezime", "Novi Sad"
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.register(request));

        assertEquals("Lozinke se ne poklapaju", ex.getMessage());
        verifyNoInteractions(userRepository);
    }

    // Test 2: Registracija odbija kad email vec postoji (mock UserRepository)
    @Test
    void register_throwsWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com", "username123", "pass123", "pass123",
                "Ime", "Prezime", "Novi Sad"
        );
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.register(request));

        assertEquals("Email adresa je već u upotrebi", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // Test 3: Login baca BadRequestException kad AuthenticationManager baci BadCredentialsException
    @Test
    void login_throwsBadRequestExceptionOnWrongCredentials() {
        LoginRequest request = new LoginRequest("user@example.com", "wrongpassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.login(request));

        assertTrue(ex.getMessage().contains("Pogrešan email ili lozinka"));
    }
}
