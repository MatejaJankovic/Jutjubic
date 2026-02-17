package rs.ftn.isa.mqconsumer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ConsumerMetricsService {

    private final ConsumerMetrics metrics = new ConsumerMetrics();
    private boolean reportPrinted = false;

    public void recordJsonMetric(double time, int size) {
        metrics.addJsonMetric(time, size);
        reportPrinted = false;
    }

    public void recordProtobufMetric(double time, int size) {
        metrics.addProtobufMetric(time, size);
        reportPrinted = false;
    }

    public ConsumerMetrics getMetrics() {
        return metrics;
    }

    public void reset() {
        metrics.getJsonDeserializationTimes().clear();
        metrics.getProtobufDeserializationTimes().clear();
        metrics.getJsonMessageSizes().clear();
        metrics.getProtobufMessageSizes().clear();
        reportPrinted = false;
    }

    @Scheduled(fixedDelay = 3000)
    public void checkAndPrintReport() {
        if (metrics.hasMessages() &&
                metrics.getSecondsSinceLastMessage() >= 5 &&
                !reportPrinted) {

            printReport();
            reportPrinted = true;
        }
    }

    private void printReport() {
        int count = metrics.getTotalMessages();

        System.out.println("\n");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("          ANALIZA");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println();

        System.out.println("PRIMLJENE PORUKE: " + count + " (JSON) + " + count + " (Protobuf)");
        System.out.println();

        System.out.println("PROSEČNO VREME DESERIJALIZACIJE:");
        System.out.println("   JSON:     " + String.format("%.6f ms", metrics.getAverageJsonDeserializationTime()));
        System.out.println("   Protobuf: " + String.format("%.6f ms", metrics.getAverageProtobufDeserializationTime()));
        System.out.println();

        System.out.println("PROSEČNA VELIČINA PORUKE:");
        System.out.println("   JSON:     " + String.format("%.2f B", metrics.getAverageJsonMessageSize()));
        System.out.println("   Protobuf: " + String.format("%.2f B", metrics.getAverageProtobufMessageSize()));
        System.out.println();

        System.out.println("POREĐENJE:");

        double timeRatio = metrics.getAverageProtobufDeserializationTime() /
                metrics.getAverageJsonDeserializationTime();
        if (timeRatio > 1) {
            System.out.println("   Protobuf je " + String.format("%.1fx", timeRatio) + " SPORIJI u deserijalizaciji");
        } else {
            System.out.println("   Protobuf je " + String.format("%.1fx", 1/timeRatio) + " BRŽI u deserijalizaciji");
        }

        double sizeReduction = ((metrics.getAverageJsonMessageSize() -
                metrics.getAverageProtobufMessageSize()) /
                metrics.getAverageJsonMessageSize()) * 100;
        System.out.println("   Protobuf je " + String.format("%.1f%%", sizeReduction) + " MANJI");

        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println();
    }
}