package rs.ftn.isa.jutjubicbackend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.isa.jutjubicbackend.dto.TranscodingMessage;
import rs.ftn.isa.jutjubicbackend.model.TranscodingJob;
import rs.ftn.isa.jutjubicbackend.repository.TranscodingJobRepository;
import rs.ftn.isa.jutjubicbackend.service.TranscodingService;

import java.time.Instant;

/**
 * Consumer that processes video transcoding requests from RabbitMQ
 * Implements idempotency to prevent duplicate processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranscodingConsumer {

    private static final String QUEUE = "video.transcoding.queue";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_PROCESSED = "PROCESSED";
    private static final String STATUS_FAILED = "FAILED";

    private final TranscodingJobRepository transcodingJobRepository;
    private final TranscodingService transcodingService;

    /**
     * Listen to transcoding queue with 2 concurrent consumers for parallel processing
     * Implements idempotency using TranscodingJob entity
     */
    @RabbitListener(queues = QUEUE, concurrency = "2")
    public void handleTranscodingRequest(TranscodingMessage message) {
        Long videoId = message.getVideoId();
        String threadName = Thread.currentThread().getName();

        log.info("📨 Picked job videoId={} by consumer thread={}", videoId, threadName);

        // Step 1: Try to claim the job (idempotency check)
        TranscodingJob job;
        try {
            job = claimJob(videoId);
            if (job == null) {
                log.warn("⚠️ Skipped duplicate message videoId={} - already being processed or completed", videoId);
                return; // Acknowledge message without reprocessing
            }
        } catch (Exception e) {
            log.error("💥 Failed to claim job for videoId={}: {}", videoId, e.getMessage());
            return; // Acknowledge and skip this message
        }

        // Step 2: Perform actual transcoding
        try {
            transcodingService.process(message);

            // Step 3a: On success - mark as PROCESSED
            markJobAsProcessed(videoId);
            log.info("Transcoding finished videoId={} status={} thread={}",
                    videoId, STATUS_PROCESSED, threadName);

        } catch (Exception transcodingError) {
            // Step 3b: On failure - mark as FAILED and save error
            markJobAsFailed(videoId, transcodingError.getMessage());
            log.error("Transcoding failed videoId={} status={} error={}",
                    videoId, STATUS_FAILED, transcodingError.getMessage());
            // DO NOT rethrow - we acknowledge the message to prevent infinite retry
        }
    }

    /**
     * Attempt to claim a job for processing (idempotency check)
     * Returns null if job already exists (duplicate message)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected TranscodingJob claimJob(Long videoId) {
        // Check if job already exists
        if (transcodingJobRepository.findByVideoId(videoId).isPresent()) {
            return null; // Already claimed
        }

        // Create new job
        try {
            TranscodingJob job = TranscodingJob.builder()
                    .videoId(videoId)
                    .status(STATUS_IN_PROGRESS)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            transcodingJobRepository.save(job);
            log.info("✓ Created transcoding job record for videoId={}", videoId);
            return job;

        } catch (DataIntegrityViolationException e) {
            // Race condition: another consumer claimed it first
            log.warn("⚠️ Race condition: job for videoId={} was claimed by another consumer", videoId);
            return null;
        }
    }

    /**
     * Mark job as successfully processed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markJobAsProcessed(Long videoId) {
        transcodingJobRepository.findByVideoId(videoId).ifPresent(job -> {
            job.setStatus(STATUS_PROCESSED);
            job.setUpdatedAt(Instant.now());
            transcodingJobRepository.save(job);
        });
    }

    /**
     * Mark job as failed with error message
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markJobAsFailed(Long videoId, String errorMessage) {
        // Truncate error message if too long (must be done before lambda)
        final String finalErrorMessage = (errorMessage != null && errorMessage.length() > 2000)
                ? errorMessage.substring(0, 2000)
                : errorMessage;

        transcodingJobRepository.findByVideoId(videoId).ifPresent(job -> {
            job.setStatus(STATUS_FAILED);
            job.setLastError(finalErrorMessage);
            job.setUpdatedAt(Instant.now());
            transcodingJobRepository.save(job);
        });
    }
}

