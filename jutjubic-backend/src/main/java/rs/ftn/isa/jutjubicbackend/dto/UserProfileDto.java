// java
// File: jutjubic-backend/src/main/java/rs/ftn/isa/jutjubicbackend/dto/UserProfileDto.java
package rs.ftn.isa.jutjubicbackend.dto;

public class UserProfileDto {
    private String username;
    private String firstName;
    private String lastName;

    public UserProfileDto() {}

    public UserProfileDto(String username, String firstName, String lastName) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public static UserProfileDto fromEntity(rs.ftn.isa.jutjubicbackend.model.User user) {
        return new UserProfileDto(user.getUsername(), user.getFirstName(), user.getLastName());
    }
}
