package rs.ftn.isa.jutjubicbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtev za registraciju novog korisnika")
public class RegisterRequest {

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email format nije validan")
    @Schema(description = "Email adresa korisnika", example = "marko@example.com")
    private String email;

    @NotBlank(message = "Korisničko ime je obavezno")
    @Size(min = 3, max = 50, message = "Korisničko ime mora imati između 3 i 50 karaktera")
    @Schema(description = "Korisničko ime (jedinstveno)", example = "marko123", minLength = 3, maxLength = 50)
    private String username;

    @NotBlank(message = "Lozinka je obavezna")
    @Size(min = 6, message = "Lozinka mora imati najmanje 6 karaktera")
    @Schema(description = "Lozinka", example = "password123", minLength = 6)
    private String password;

    @NotBlank(message = "Potvrda lozinke je obavezna")
    @Schema(description = "Potvrda lozinke (mora biti ista kao lozinka)", example = "password123")
    private String confirmPassword;

    @NotBlank(message = "Ime je obavezno")
    @Schema(description = "Ime korisnika", example = "Marko")
    private String firstName;

    @NotBlank(message = "Prezime je obavezno")
    @Schema(description = "Prezime korisnika", example = "Petrović")
    private String lastName;

    @NotBlank(message = "Adresa je obavezna")
    @Schema(description = "Adresa korisnika", example = "Novi Sad, Srbija")
    private String address;
}



