package rs.ftn.isa.jutjubicbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ETLSchedulerService {

    private final ETLService etlService;

    /**
     * Runs ETL pipeline every day at midnight (00:00)
     * Cron format: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledETLPipeline() {
        log.info("Starting scheduled ETL Pipeline execution");
        try {
            etlService.runETLPipeline();
            log.info("Scheduled ETL Pipeline completed successfully");
        } catch (Exception e) {
            log.error("Scheduled ETL Pipeline failed", e);
        }
    }

    /**
     * For testing purposes - runs every 5 minutes
     * Uncomment this and comment the daily schedule above for testing
     */
    // @Scheduled(cron = "0 */5 * * * *")
    // public void testScheduledETLPipeline() {
    //     log.info("Starting TEST scheduled ETL Pipeline execution (every 5 minutes)");
    //     try {
    //         etlService.runETLPipeline();
    //         log.info("TEST scheduled ETL Pipeline completed successfully");
    //     } catch (Exception e) {
    //         log.error("TEST scheduled ETL Pipeline failed", e);
    //     }
    // }
}

