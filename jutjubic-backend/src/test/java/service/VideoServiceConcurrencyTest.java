package service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.isa.jutjubicbackend.JutjubicBackendApplication;
import rs.ftn.isa.jutjubicbackend.model.Video;
import rs.ftn.isa.jutjubicbackend.repository.VideoRepository;
import rs.ftn.isa.jutjubicbackend.service.VideoService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = JutjubicBackendApplication.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class VideoServiceConcurrencyTest {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoRepository videoRepository;

    @Test
    void testConcurrentViewIncrementExistingVideo() throws InterruptedException {
        Long existingVideoId = 13L;
        Video video = videoRepository.findById(existingVideoId)
                .orElseThrow(() -> new RuntimeException("Video ne postoji u bazi"));

        System.out.println("View count pre testa: " + video.getViewCount());

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                videoService.incrementViewCount(video.getId());
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        Video updatedVideo = videoRepository.findById(video.getId()).get();
        System.out.println("View count nakon testa: " + updatedVideo.getViewCount());

        assertEquals(video.getViewCount() + threads, updatedVideo.getViewCount(),
                "View count se nije pravilno povećao!");
    }
}
