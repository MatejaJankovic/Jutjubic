package rs.ftn.isa.jutjubicbackend.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.ftn.isa.jutjubicbackend.model.UploadEvent;
import rs.ftn.isa.jutjubicbackend.protobuf.UploadEventProto;

@Service
public class UploadEventProtobufProducer {

    private final RabbitTemplate rabbitTemplate;


    public UploadEventProtobufProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendProtobufEvent(UploadEvent event) {
        long start = System.nanoTime();

        UploadEventProto.UploadEvent proto =
                UploadEventProto.UploadEvent.newBuilder()
                        .setTitle(event.getTitle())
                        .setAuthor(event.getAuthor())
                        .setSize(event.getSize())
                        .setTimestamp(event.getTimestamp())
                        .build();

        byte[] bytes = proto.toByteArray();
        rabbitTemplate.convertAndSend("upload-protobuf-queue", bytes);

        long end = System.nanoTime();
        double durationMs = (end - start) / 1_000_000.0;

        System.out.println("Protobuf poslat: " + proto.getTitle() +
                ", serijalizacija: " + String.format("%.3f", durationMs) + " ms, " +
                "veličina: " + bytes.length + " B");
    }
}