package rs.ftn.isa.jutjubicbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import rs.ftn.isa.jutjubicbackend.model.WatchParty;
import rs.ftn.isa.jutjubicbackend.repository.WatchPartyRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchPartyServiceTest {

    @Mock private WatchPartyRepository watchPartyRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WatchPartyService watchPartyService;

    // Test 4: createRoom kreira sobu sa UUID invite kodom i dodaje kreatora kao ucesnika
    @Test
    void createRoom_savesRoomWithUUIDInviteCodeAndAddsCreatorAsParticipant() {
        when(watchPartyRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(watchPartyRepository.save(any(WatchParty.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WatchParty result = watchPartyService.createRoom(42L, 1L, true);

        assertNotNull(result.getInviteCode());
        assertTrue(result.getInviteCode().matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "Invite kod treba biti UUID format");
        assertTrue(result.getParticipantIds().contains(42L),
                "Kreator treba biti dodat kao ucesnik");
        verify(watchPartyRepository).save(any(WatchParty.class));
    }

    // Test 5: joinRoom baca RuntimeException kad invite kod ne postoji (mock repository vraca prazan Optional)
    @Test
    void joinRoom_throwsRuntimeExceptionWhenInviteCodeNotFound() {
        when(watchPartyRepository.findByInviteCode("nepostojeci-kod"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> watchPartyService.joinRoom("nepostojeci-kod", 1L));

        assertTrue(ex.getMessage().contains("Watch party not found"));
        verify(watchPartyRepository, never()).save(any());
    }

    // Test 6: switchVideo baca RuntimeException kad korisnik nije kreator sobe (mock repository + provera autorizacije)
    @Test
    void switchVideo_throwsWhenUserIsNotCreator() {
        Long roomId = 1L;
        Long creatorId = 10L;
        Long nonCreatorId = 99L;
        Long newVideoId = 5L;

        WatchParty room = WatchParty.builder()
                .id(roomId)
                .creatorId(creatorId)
                .videoId(1L)
                .inviteCode("some-code")
                .createdAt(Instant.now())
                .participantIds(new HashSet<>())
                .build();

        when(watchPartyRepository.findById(roomId)).thenReturn(Optional.of(room));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> watchPartyService.switchVideo(roomId, newVideoId, nonCreatorId));

        assertTrue(ex.getMessage().contains("Only the room owner can switch videos"));
        verify(watchPartyRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
