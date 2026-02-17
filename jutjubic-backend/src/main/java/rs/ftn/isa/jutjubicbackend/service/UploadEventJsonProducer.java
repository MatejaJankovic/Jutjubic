package rs.ftn.isa.jutjubicbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.ftn.isa.jutjubicbackend.model.UploadEvent;

@Service
public class UploadEventJsonProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UploadEventJsonProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendJsonEvent(UploadEvent event) {
        long start = System.nanoTime();

        rabbitTemplate.convertAndSend("upload-json-queue", event);

        long end = System.nanoTime();
        double durationMs = (end - start) / 1_000_000.0;

        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(event);
            int size = jsonBytes.length;


            System.out.println("JSON poslat: " + event.getTitle() +
                    ", serijalizacija: " + String.format("%.3f", durationMs) + " ms, " +
                    "veličina: " + size + " B");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}