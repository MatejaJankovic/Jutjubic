package rs.ftn.isa.jutjubicbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.ftn.isa.jutjubicbackend.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByActivationToken(String activationToken);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}



