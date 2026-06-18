package rs.ftn.isa.jutjubicbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import rs.ftn.isa.jutjubicbackend.api.AuthController;
import rs.ftn.isa.jutjubicbackend.config.SecurityConfig;
import rs.ftn.isa.jutjubicbackend.dto.AuthResponse;
import rs.ftn.isa.jutjubicbackend.dto.LoginRequest;
import rs.ftn.isa.jutjubicbackend.dto.RegisterRequest;
import rs.ftn.isa.jutjubicbackend.exception.BadRequestException;
import rs.ftn.isa.jutjubicbackend.security.JwtTokenProvider;
import rs.ftn.isa.jutjubicbackend.security.LoginRateLimiterService;
import rs.ftn.isa.jutjubicbackend.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integracioni testovi za AuthController.
 *
 * Koristi @WebMvcTest koji pokrece PRAVI Spring MVC kontekst sa:
 * - Jackson JSON serijalizacija/deserijalizacija
 * - @Valid Bean Validation (Hibernate Validator)
 * - Spring Security filter chain (JwtAuthenticationFilter + SecurityConfig pravila)
 * - GlobalExceptionHandler (pretvara izuzetke u HTTP odgovore)
 *
 * Mokujemo SAMO: AuthService i LoginRateLimiterService (zahtevaju bazu/stanje),
 * JwtTokenProvider i UserDetailsService (zahtevaju bazu i konfiguraciju kljuca).
 * Sve ostalo je REALNO - testiramo pravu integraciju slojeva.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private LoginRateLimiterService rateLimiterService;

    // Potrebno za JwtAuthenticationFilter koji Spring Security ucitava u filter chain
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    // Potrebno za SecurityConfig i JwtAuthenticationFilter
    @MockBean
    private UserDetailsService userDetailsService;

    // Test IT-1: Uspesna registracija - prolazi kroz ceo MVC pipeline i vraca 201 sa success:true
    @Test
    void register_returns201WithSuccessMessage() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "test@example.com", "testuser", "pass123", "pass123",
                "Test", "User", "Novi Sad"
        );
        doNothing().when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Registracija uspešna! Proverite vašu email inbox radi verifikacije naloga."));
    }

    // Test IT-2: @Valid anotacija + GlobalExceptionHandler vracaju 400 sa mapom 'errors' za nevalidan unos
    // Proverava integraciju: Spring MVC validacija -> GlobalExceptionHandler -> ValidationErrorResponse JSON
    @Test
    void register_returns400WithValidationErrorsWhenBodyIsInvalid() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "ne-email", "ab", "p", "p", "", "", ""
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors").exists());
    }

    // Test IT-3: BadRequestException iz servisa se mapira kroz GlobalExceptionHandler u 400 ErrorResponse
    // Proverava integraciju: AuthController -> GlobalExceptionHandler -> ErrorResponse JSON
    @Test
    void register_returns400WithMessageWhenServiceThrowsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com", "existinguser", "pass123", "pass123",
                "Test", "User", "Novi Sad"
        );
        doThrow(new BadRequestException("Email adresa je već u upotrebi"))
                .when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email adresa je već u upotrebi"));
    }

    // Test IT-4: Uspesna prijava - AuthController integrisan sa rateLimiterom, vraca 200 sa JWT tokenom
    // Proverava integraciju: rateLimiterService.isBlocked() -> authService.login() -> AuthResponse JSON
    @Test
    void login_returns200WithTokenWhenCredentialsAreValid() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "pass123");
        AuthResponse authResponse = AuthResponse.builder()
                .token("eyJhbGciOiJIUzI1NiJ9.test.signature")
                .type("Bearer")
                .id(1L)
                .email("user@example.com")
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .role("USER")
                .build();

        when(rateLimiterService.isBlocked(any())).thenReturn(false);
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiJ9.test.signature"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    // Test IT-5: Rate limiter blokira IP -> RateLimitExceededException -> GlobalExceptionHandler -> 429 sa Retry-After headerom
    // Proverava integraciju: LoginRateLimiterService -> AuthController -> exception -> HTTP header + status
    @Test
    void login_returns429WithRetryAfterHeaderWhenIpIsBlocked() throws Exception {
        LoginRequest request = new LoginRequest("blocked@example.com", "pass123");

        when(rateLimiterService.isBlocked(any())).thenReturn(true);
        when(rateLimiterService.getSecondsUntilReset(any())).thenReturn(42L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }
}
