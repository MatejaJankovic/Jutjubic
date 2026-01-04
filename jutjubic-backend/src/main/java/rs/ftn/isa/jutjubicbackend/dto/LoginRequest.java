package rs.ftn.isa.jutjubicbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtev za prijavu korisnika")
public class LoginRequest {

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email format nije validan")
    @Schema(description = "Email adresa korisnika", example = "marko@example.com")
    private String email;

    @NotBlank(message = "Lozinka je obavezna")
    @Schema(description = "Lozinka korisnika", example = "password123")
    private String password;
}

