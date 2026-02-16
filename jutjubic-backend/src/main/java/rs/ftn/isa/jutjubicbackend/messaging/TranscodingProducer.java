package rs.ftn.isa.jutjubicbackend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import rs.ftn.isa.jutjubicbackend.dto.TranscodingMessage;

import java.time.Instant;
import java.util.List;

/**
 * Producer that publishes video transcoding requests to RabbitMQ
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranscodingProducer {

    private static final String EXCHANGE = "video.transcoding.exchange";
    private static final String ROUTING_KEY = "video.transcoding";

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish a transcoding request for a newly uploaded video
     *
     * @param videoId    ID of the video entity
     * @param inputPath  Absolute or relative path to the uploaded video file
     */
    public void publishTranscodingRequest(Long videoId, String inputPath) {
        List<String> requestedProfiles = List.of("720p", "1080p");

        TranscodingMessage message = TranscodingMessage.builder()
                .videoId(videoId)
                .inputPath(inputPath)
                .requestedProfiles(requestedProfiles)
                .createdAt(Instant.now())
                .build();

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, message);

            log.info("Published transcoding request: videoId={}, path={}, profiles={}",
                    videoId, inputPath, requestedProfiles);

        } catch (Exception e) {
            log.error("Failed to publish transcoding request for videoId={}: {}",
                    videoId, e.getMessage(), e);
            // Don't throw - we don't want to fail the entire upload if messaging fails
            // The transcoding can be triggered manually or retried later
        }
    }
}

