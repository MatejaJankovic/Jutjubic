package rs.ftn.isa.jutjubicbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import rs.ftn.isa.jutjubicbackend.api.WatchPartyController;
import rs.ftn.isa.jutjubicbackend.config.SecurityConfig;
import rs.ftn.isa.jutjubicbackend.model.WatchParty;
import rs.ftn.isa.jutjubicbackend.security.JwtTokenProvider;
import rs.ftn.isa.jutjubicbackend.service.VideoService;
import rs.ftn.isa.jutjubicbackend.service.WatchPartyService;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integracioni testovi za WatchPartyController.
 *
 * Testira pravu Spring Security konfiguraciju (SecurityConfig pravila) integrisan sa
 * WatchPartyController-om kroz MockMvc. Proverava da security filter chain ispravno:
 * - blokira neautentifikovane zahteve na zasticenim endpointima (403)
 * - propusta autentifikovane zahteve dalje do kontrolera
 *
 * Mokujemo SAMO: WatchPartyService, VideoService (zahtevaju bazu),
 * JwtTokenProvider, UserDetailsService (zahtevaju konfiguraciju/bazu).
 * Spring Security konfiguracija ostaje REALNA.
 */
@WebMvcTest(WatchPartyController.class)
@Import(SecurityConfig.class)
class WatchPartyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WatchPartyService watchPartyService;

    @MockBean
    private VideoService videoService;

    // Potrebno za JwtAuthenticationFilter koji Spring Security ucitava u filter chain
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    // Potrebno za SecurityConfig i JwtAuthenticationFilter
    @MockBean
    private UserDetailsService userDetailsService;

    // Test IT-6: Spring Security odbija POST /watch-party/create bez JWT tokena sa 403
    // Proverava integraciju: REALNA SecurityConfig -> JwtAuthenticationFilter -> anyRequest().authenticated()
    @Test
    void createWatchParty_returns403WhenRequestHasNoJwtToken() throws Exception {
        mockMvc.perform(post("/api/watch-party/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"videoId\":1,\"public\":true}"))
                .andExpect(status().isForbidden());
    }

    // Test IT-7: GET /watch-party/{id} za autentifikovanog korisnika - vraca 404 kad soba ne postoji
    // Proverava integraciju: @WithMockUser (autentifikacija) -> kontroler -> WatchPartyService.findById() -> 404
    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getWatchParty_returns404WhenRoomDoesNotExist() throws Exception {
        when(watchPartyService.findById(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/watch-party/999"))
                .andExpect(status().isNotFound());
    }

    // Test IT-8: GET /watch-party/{id} za autentifikovanog korisnika - vraca 200 sa ispravnim JSON-om
    // Proverava integraciju: WatchPartyService.findById() -> WatchPartyDTO.fromEntity() -> Jackson serijalizacija -> JSON
    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getWatchParty_returns200WithDtoWhenRoomExists() throws Exception {
        WatchParty room = WatchParty.builder()
                .id(1L)
                .creatorId(42L)
                .videoId(5L)
                .inviteCode("abc-123-def")
                .isPublic(true)
                .participantIds(new HashSet<>())
                .createdAt(Instant.now())
                .build();

        when(watchPartyService.findById(1L)).thenReturn(Optional.of(room));

        mockMvc.perform(get("/api/watch-party/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.inviteCode").value("abc-123-def"))
                .andExpect(jsonPath("$.public").value(true));
    }
}
