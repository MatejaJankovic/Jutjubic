package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.isa.jutjubicbackend.model.User;
import rs.ftn.isa.jutjubicbackend.model.Video;
import rs.ftn.isa.jutjubicbackend.repository.UserRepository;
import rs.ftn.isa.jutjubicbackend.repository.VideoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestDataGenerator {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private static final String[] TITLES = {
            "Amazing Sunset in", "Beautiful Landscape of", "Exploring", "Hidden Gems of",
            "Travel Vlog:", "Walking Tour of", "Street Food in", "Architecture of",
            "Day Trip to", "Best Views in", "Local Culture of", "Historic Sites in",
            "Night Walk through", "Weekend in", "Discovering", "Must-See Places in"
    };

    private static final String[] CITIES = {
            "Paris", "Rome", "London", "Berlin", "Barcelona", "Amsterdam", "Vienna",
            "Prague", "Budapest", "Istanbul", "Athens", "Dubrovnik", "Edinburgh",
            "Copenhagen", "Stockholm", "Oslo", "Helsinki", "Warsaw", "Krakow",
            "Brussels", "Munich", "Venice", "Florence", "Milan", "Lisbon", "Porto",
            "Madrid", "Seville", "Valencia", "Dublin", "Reykjavik", "Zurich",
            "Geneva", "Salzburg", "Innsbruck", "Ljubljana", "Zagreb", "Belgrade",
            "Bucharest", "Sofia", "Sarajevo", "Novi Sad", "Split", "Tallinn", "Riga"
    };

    private static final String[] DESCRIPTIONS = {
            "Join me as I explore the beautiful streets and discover hidden gems!",
            "An incredible journey through one of Europe's most stunning destinations.",
            "Experience the local culture, food, and amazing architecture in this travel vlog.",
            "Walking through historic streets and capturing breathtaking views.",
            "A perfect day exploring the city's most iconic landmarks and attractions.",
            "Discovering the best local spots, amazing food, and unforgettable moments.",
            "From sunrise to sunset, experiencing everything this place has to offer.",
            "Immersing in the local culture and traditions of this beautiful European city."
    };

    private static final String[] TAG_POOL = {
            "travel", "vlog", "adventure", "europe", "city", "landscape",
            "culture", "food", "architecture", "history", "tourism", "vacation",
            "sightseeing", "wanderlust", "explore", "journey", "discovery"
    };

    /**
     * Generiše test videe sa random koordinatama širom Evrope
     *
     * @param count broj videa za generisanje (default 5000)
     */
    @Transactional
    public void generateTestVideos(int count) {
        log.info("Starting test data generation - creating {} videos", count);

        // Dobavi test korisnika ili kreiraj novog
        User testUser = getOrCreateTestUser();

        Random random = new Random();
        int batchSize = 100;
        List<Video> batch = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Video video = createTestVideo(i, testUser, random);
            batch.add(video);

            // Batch insert na svakih 100 videa
            if (batch.size() >= batchSize) {
                videoRepository.saveAll(batch);
                batch.clear();
                log.info("Progress: {}/{} videos generated", i + 1, count);
            }
        }

        // Sačuvaj ostatak
        if (!batch.isEmpty()) {
            videoRepository.saveAll(batch);
        }

        log.info("Successfully generated {} test videos", count);
    }

    private Video createTestVideo(int index, User user, Random random) {
        String city = CITIES[random.nextInt(CITIES.length)];
        String title = TITLES[random.nextInt(TITLES.length)] + " " + city;
        String description = DESCRIPTIONS[random.nextInt(DESCRIPTIONS.length)];

        return Video.builder()
                .title(title)
                .description(description)
                .tags(generateTags(random))
                .location(city)
                .latitude(generateLatitude(random))
                .longitude(generateLongitude(random))
                .videoUrl("/uploads/videos/test-video.mp4")
                .thumbnailUrl("/uploads/thumbnails/test-thumbnail.jpg")
                .user(user)
                .viewCount(generateViewCount(random))
                .likeCount(0L)
                .commentCount(0L)
                .createdAt(generateCreatedAt(random))
                .build();
    }

    private User getOrCreateTestUser() {
        return userRepository.findByUsername("testuser")
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username("testuser")
                            .email("testuser@jutjubic.com")
                            .password("$2a$10$DUMMY_PASSWORD_HASH")
                            .firstName("Test")
                            .lastName("User")
                            .address("Test Address")
                            .enabled(true)
                            .role(rs.ftn.isa.jutjubicbackend.model.Role.USER)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    /**
     * Generiše random latitude za teritoriju Evrope (35°N - 70°N)
     */
    private Double generateLatitude(Random random) {
        return 35.0 + (random.nextDouble() * 35.0); // 35°N to 70°N
    }

    /**
     * Generiše random longitude za teritoriju Evrope (-10°W - 40°E)
     */
    private Double generateLongitude(Random random) {
        return -10.0 + (random.nextDouble() * 50.0); // -10°W to 40°E
    }

    /**
     * Generiše random tagove za video
     */
    private List<String> generateTags(Random random) {
        int tagCount = random.nextInt(3) + 2; // 2-4 taga
        List<String> selectedTags = new ArrayList<>();

        for (int i = 0; i < tagCount; i++) {
            String tag = TAG_POOL[random.nextInt(TAG_POOL.length)];
            if (!selectedTags.contains(tag)) {
                selectedTags.add(tag);
            }
        }

        return selectedTags;
    }

    /**
     * Generiše random broj pregleda
     */
    private Long generateViewCount(Random random) {
        // Većina videa ima mali broj pregleda, neki su popularni
        double roll = random.nextDouble();
        if (roll < 0.7) {
            // 70% videa: 0-1000 pregleda
            return (long) random.nextInt(1000);
        } else if (roll < 0.9) {
            // 20% videa: 1000-10000 pregleda
            return 1000L + random.nextInt(9000);
        } else {
            // 10% videa: 10000-100000 pregleda (popularni)
            return 10000L + random.nextInt(90000);
        }
    }

    /**
     * Generiše random datum kreiranja (zadnjih 12 meseci)
     */
    private LocalDateTime generateCreatedAt(Random random) {
        LocalDateTime now = LocalDateTime.now();
        int daysAgo = random.nextInt(365); // Zadnjih 12 meseci
        int hoursOffset = random.nextInt(24);
        int minutesOffset = random.nextInt(60);

        return now.minusDays(daysAgo)
                .minusHours(hoursOffset)
                .minusMinutes(minutesOffset);
    }

    /**
     * Briše sve test videe
     */
    @Transactional
    public void deleteAllTestVideos() {
        log.info("Deleting all test videos");
        User testUser = userRepository.findByUsername("testuser").orElse(null);
        if (testUser != null) {
            List<Video> testVideos = videoRepository.findByUserIdOrderByCreatedAtDesc(
                    testUser.getId(),
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)
            ).getContent();

            videoRepository.deleteAll(testVideos);
            log.info("Deleted {} test videos", testVideos.size());
        }
    }
}

