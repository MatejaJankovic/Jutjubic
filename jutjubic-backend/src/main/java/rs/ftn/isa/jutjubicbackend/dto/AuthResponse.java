package rs.ftn.isa.jutjubicbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Odgovor nakon uspešne prijave sa JWT tokenom")
public class AuthResponse {

    @Schema(description = "JWT token za autentifikaciju", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Tip tokena", example = "Bearer")
    private String type = "Bearer";

    @Schema(description = "ID korisnika", example = "1")
    private Long id;

    @Schema(description = "Email adresa", example = "marko@example.com")
    private String email;

    @Schema(description = "Korisničko ime", example = "marko123")
    private String username;

    @Schema(description = "Ime korisnika", example = "Marko")
    private String firstName;

    @Schema(description = "Prezime korisnika", example = "Petrović")
    private String lastName;

    @Schema(description = "Uloga korisnika", example = "USER")
    private String role;
}

