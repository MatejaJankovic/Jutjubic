package rs.ftn.isa.mqconsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UploadEventConsumer {

    @Autowired
    private ConsumerMetricsService metricsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(
            queues = "upload-json-queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void receive(UploadEvent event) {
        try {
            long start = System.nanoTime();

            String title = event.getTitle();

            long end = System.nanoTime();
            double durationMs = (end - start) / 1_000_000.0;

            // Izračunaj veličinu
            byte[] jsonBytes = objectMapper.writeValueAsBytes(event);
            int size = jsonBytes.length;

            metricsService.recordJsonMetric(durationMs, size);

            System.out.println("JSON primljen: " + title +
                    ", deserijalizacija: " + String.format("%.4f", durationMs) + " ms, " +
                    "veličina: " + size + " B");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}