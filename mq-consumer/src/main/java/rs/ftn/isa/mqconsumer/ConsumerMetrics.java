package rs.ftn.isa.mqconsumer;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ConsumerMetrics {
    private List<Double> jsonDeserializationTimes = new ArrayList<>();
    private List<Double> protobufDeserializationTimes = new ArrayList<>();
    private List<Integer> jsonMessageSizes = new ArrayList<>();
    private List<Integer> protobufMessageSizes = new ArrayList<>();
    private long lastMessageTime = System.currentTimeMillis();

    public void addJsonMetric(double time, int size) {
        jsonDeserializationTimes.add(time);
        jsonMessageSizes.add(size);
        lastMessageTime = System.currentTimeMillis();
    }

    public void addProtobufMetric(double time, int size) {
        protobufDeserializationTimes.add(time);
        protobufMessageSizes.add(size);
        lastMessageTime = System.currentTimeMillis();
    }

    public double getAverageJsonDeserializationTime() {
        if (jsonDeserializationTimes.size() <= 1) return 0;
        return jsonDeserializationTimes.stream()
                .skip(1)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public double getAverageProtobufDeserializationTime() {
        if (protobufDeserializationTimes.size() <= 1) return 0;
        return protobufDeserializationTimes.stream()
                .skip(1)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public double getAverageJsonMessageSize() {
        return jsonMessageSizes.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    public double getAverageProtobufMessageSize() {
        return protobufMessageSizes.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    public int getTotalMessages() {
        return jsonDeserializationTimes.size();
    }

    public boolean hasMessages() {
        return !jsonDeserializationTimes.isEmpty() && !protobufDeserializationTimes.isEmpty();
    }

    public long getSecondsSinceLastMessage() {
        return (System.currentTimeMillis() - lastMessageTime) / 1000;
    }
}