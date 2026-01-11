package rs.ftn.isa.jutjubicbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import rs.ftn.isa.jutjubicbackend.model.Role;
import rs.ftn.isa.jutjubicbackend.model.User;
import rs.ftn.isa.jutjubicbackend.model.Video;
import rs.ftn.isa.jutjubicbackend.repository.UserRepository;
import rs.ftn.isa.jutjubicbackend.repository.VideoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (videoRepository.count() == 0) {
            log.info("No videos found, creating sample data...");
            createSampleData();
        } else {
            log.info("Videos already exist, skipping sample data creation.");
        }
    }

    private void createSampleData() {

        User demoUser = userRepository.findByEmail("demo@jutjubic.rs")
                .orElseGet(() -> {
                    User user = User.builder()
                            .email("demo@jutjubic.rs")
                            .username("demo")
                            .password(passwordEncoder.encode("demo123"))
                            .firstName("Demo")
                            .lastName("Korisnik")
                            .address("Novi Sad, Srbija")
                            .enabled(true)
                            .role(Role.USER)
                            .build();
                    return userRepository.save(user);
                });


        List<Video> videos = List.of(
                Video.builder()
                        .title("Uvod u programiranje - Java za početnike")
                        .description("Naučite osnove Java programiranja kroz praktične primere. Ovaj video pokriva osnovne koncepte, tipove podataka, kontrolne strukture i više.")
                        .videoUrl("https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4")
                        .thumbnailUrl("https://picsum.photos/seed/video1/640/360")
                        .durationSeconds(3650)
                        .viewCount(15420L)
                        .likeCount(892L)
                        .commentCount(124L)
                        .user(demoUser)
                        .build(),
                Video.builder()
                        .title("Spring Boot Tutorial - REST API od nule")
                        .description("Kompletni vodič za kreiranje REST API-ja sa Spring Boot frameworkom. Uključuje JPA, Security i više.")
                        .videoUrl("https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4")
                        .thumbnailUrl("https://picsum.photos/seed/video2/640/360")
                        .durationSeconds(5420)
                        .viewCount(23150L)
                        .likeCount(1547L)
                        .commentCount(289L)
                        .user(demoUser)
                        .build(),
                Video.builder()
                        .title("Angular 18 - Moderni frontend development")
                        .description("Savladajte Angular 18 sa najnovijim funkcionalnostima kao što su signals, standalone komponente i više.")
                        .videoUrl("https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4")
                        .thumbnailUrl("https://picsum.photos/seed/video3/640/360")
                        .durationSeconds(4200)
                        .viewCount(18920L)
                        .likeCount(1230L)
                        .commentCount(198L)
                        .user(demoUser)
                        .build(),
                Video.builder()
                        .title("PostgreSQL - Napredne tehnike baza podataka")
                        .description("Optimizacija upita, indeksi, transakcije i napredne funkcionalnosti PostgreSQL baze podataka.")
                        .videoUrl("https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4")
                        .thumbnailUrl("https://picsum.photos/seed/video4/640/360")
                        .durationSeconds(2890)
                        .viewCount(8745L)
                        .likeCount(456L)
                        .commentCount(67L)
                        .user(demoUser)
                        .build(),
                Video.builder()
                        .title("Docker i Kubernetes za programere")
                        .description("Naučite kako da kontejnerizujete vaše aplikacije i upravljate njima pomoću Kubernetes klastera.")
                        .videoUrl("https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4")
                        .thumbnailUrl("https://picsum.photos/seed/video5/640/360")
                        .durationSeconds(6120)
                        .viewCount(31500L)
                        .likeCount(2890L)
                        .commentCount(412L)
                        .user(demoUser)
                        .build(),
                Video.builder()
                        .title("Git i GitHub - Verzioniranje koda")
                        .description("Sve što trebate znati o Git-u: grananje, spajanje, rešavanje konflikata i rad sa GitHub-om.")
                        .videoUrl("https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4")
                        .thumbnailUrl("https://picsum.photos/seed/video6/640/360")
                        .durationSeconds(2340)
                        .viewCount(45600L)
                        .likeCount(3210L)
                        .commentCount(534L)
                        .user(demoUser)
                        .build(),
                Video.builder()
                        .title("TypeScript - Tipiziran JavaScript")
                        .description("Zašto koristiti TypeScript? Ovaj video objašnjava prednosti i kako početi sa TypeScript-om.")
                        .videoUrl("https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4")
                        .thumbnailUrl("https://picsum.photos/seed/video7/640/360")
                        .durationSeconds(3780)
                        .viewCount(12340L)
                        .likeCount(789L)
                        .commentCount(112L)
                        .user(demoUser)
                        .build(),
                Video.builder()
                        .title("CSS Grid i Flexbox masterclass")
                        .description("Kompletni vodič za moderne CSS layout tehnike. Kreirajte responzivne dizajne sa lakoćom.")
                        .videoUrl("https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4")
                        .thumbnailUrl("https://picsum.photos/seed/video8/640/360")
                        .durationSeconds(4560)
                        .viewCount(27800L)
                        .likeCount(1980L)
                        .commentCount(287L)
                        .user(demoUser)
                        .build(),
                Video.builder()
                        .title("JWT Autentifikacija objašnjena")
                        .description("Razumite kako JWT tokeni funkcionišu i kako ih implementirati u vašim aplikacijama.")
                        .videoUrl("https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4")
                        .thumbnailUrl("https://picsum.photos/seed/video9/640/360")
                        .durationSeconds(1890)
                        .viewCount(9870L)
                        .likeCount(654L)
                        .commentCount(89L)
                        .user(demoUser)
                        .build(),
                Video.builder()
                        .title("Microservices arhitektura u praksi")
                        .description("Dizajnirajte i implementirajte mikroservisnu arhitekturu sa Spring Cloud i Netflix stack-om.")
                        .videoUrl("https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4")
                        .thumbnailUrl("https://picsum.photos/seed/video10/640/360")
                        .durationSeconds(7200)
                        .viewCount(52000L)
                        .likeCount(4120L)
                        .commentCount(623L)
                        .user(demoUser)
                        .build(),
                Video.builder()
                        .title("WebSocket - Real-time komunikacija")
                        .description("Implementirajte real-time funkcionalnosti u vašim web aplikacijama pomoću WebSocket protokola.")
                        .videoUrl("https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4")
                        .thumbnailUrl("https://picsum.photos/seed/video11/640/360")
                        .durationSeconds(3120)
                        .viewCount(14560L)
                        .likeCount(987L)
                        .commentCount(145L)
                        .user(demoUser)
                        .build(),
                Video.builder()
                        .title("Unit testiranje sa JUnit 5")
                        .description("Pišite kvalitetne unit testove za vaše Java aplikacije. Mockito, AssertJ i best practices.")
                        .videoUrl("https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4")
                        .thumbnailUrl("https://picsum.photos/seed/video12/640/360")
                        .durationSeconds(4890)
                        .viewCount(11230L)
                        .likeCount(834L)
                        .commentCount(156L)
                        .user(demoUser)
                        .build()
        );

        videoRepository.saveAll(videos);
        log.info("Created {} sample videos", videos.size());
    }
}

