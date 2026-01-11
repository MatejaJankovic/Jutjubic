package rs.ftn.isa.jutjubicbackend.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ftn.isa.jutjubicbackend.dto.AuthResponse;
import rs.ftn.isa.jutjubicbackend.dto.LoginRequest;
import rs.ftn.isa.jutjubicbackend.dto.MessageResponse;
import rs.ftn.isa.jutjubicbackend.dto.RegisterRequest;
import rs.ftn.isa.jutjubicbackend.exception.BadRequestException;
import rs.ftn.isa.jutjubicbackend.exception.RateLimitExceededException;
import rs.ftn.isa.jutjubicbackend.security.LoginRateLimiterService;
import rs.ftn.isa.jutjubicbackend.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autentifikacija", description = "API za registraciju, aktivaciju i prijavu korisnika")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiterService rateLimiterService;

    @PostMapping("/register")
    @Operation(summary = "Registracija novog korisnika",
               description = "Kreira novi korisnički nalog. Aktivacioni token se čuva u bazi i može se pronaći u konzoli ili bazi podataka.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Uspešna registracija",
                     content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validaciona greška ili email/username već postoji",
                     content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.builder()
                        .message("Registracija uspešna! Proverite vašu email inbox radi verifikacije naloga.")
                        .success(true)
                        .build());
    }

    @GetMapping("/activate")
    @Operation(summary = "Aktivacija korisničkog naloga",
               description = "Aktivira nalog koristeći token koji se nalazi u bazi podataka")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Uspešna aktivacija",
                     content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Nevažeći ili istekli token",
                     content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> activateAccount(
            @Parameter(description = "Aktivacioni token primljen putem email-a", required = true)
            @RequestParam String token) {
        authService.activateAccount(token);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Nalog je uspešno aktiviran! Sada se možete prijaviti.")
                .success(true)
                .build());
    }

    @PostMapping("/login")
    @Operation(summary = "Prijava korisnika",
               description = "Prijava korisnika sa email-om i lozinkom. Rate limiting: 5 pokušaja po minuti.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Uspešna prijava, vraća JWT token",
                     content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Pogrešni kredencijali ili nalog nije aktiviran",
                     content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "429", description = "Previše pokušaja prijave",
                     content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);


        if (rateLimiterService.isBlocked(ipAddress)) {
            long secondsUntilReset = rateLimiterService.getSecondsUntilReset(ipAddress);
            throw new RateLimitExceededException(
                    "Previše pokušaja prijave. Pokušajte ponovo za " + secondsUntilReset + " sekundi.",
                    secondsUntilReset
            );
        }

        try {
            AuthResponse response = authService.login(request);

            rateLimiterService.resetAttempts(ipAddress);
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {

            rateLimiterService.recordFailedAttempt(ipAddress);
            int remaining = rateLimiterService.getRemainingAttempts(ipAddress);
            throw new BadRequestException(e.getMessage() + " Preostalo pokušaja: " + remaining);
        }
    }

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

