package rs.ftn.isa.mqconsumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.ftn.isa.mqconsumer.protobuf.UploadEventProto;

@Component
public class UploadEventProtobufConsumer {

    @Autowired
    private ConsumerMetricsService metricsService;

    @RabbitListener(queues = "upload-protobuf-queue")
    public void receiveProtobuf(byte[] data) {
        try {
            long start = System.nanoTime();

            UploadEventProto.UploadEvent proto =
                    UploadEventProto.UploadEvent.parseFrom(data);

            long end = System.nanoTime();
            double durationMs = (end - start) / 1_000_000.0;

            metricsService.recordProtobufMetric(durationMs, data.length);

            System.out.println("Protobuf primljen: " + proto.getTitle() +
                    ", deserijalizacija: " + String.format("%.4f", durationMs) + " ms, " +
                    "veličina: " + data.length + " B");

        } catch (Exception e) {
            System.err.println("Greška: " + e.getMessage());
        }
    }
}