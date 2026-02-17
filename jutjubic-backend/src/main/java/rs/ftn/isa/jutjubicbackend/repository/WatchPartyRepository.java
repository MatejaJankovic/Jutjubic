package rs.ftn.isa.jutjubicbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.ftn.isa.jutjubicbackend.model.WatchParty;

import java.util.Optional;

@Repository
public interface WatchPartyRepository extends JpaRepository<WatchParty, Long> {

    Optional<WatchParty> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}

