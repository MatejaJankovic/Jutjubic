package rs.ftn.isa.jutjubicbackend.model;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ActiveUsersMetrics {

    private final AtomicInteger activeUsers = new AtomicInteger(0);

    public ActiveUsersMetrics(MeterRegistry registry) {
        Gauge.builder("jutjubic_active_users", activeUsers, AtomicInteger::get)
                .description("Number of currently active users")
                .register(registry);
    }

    public void userLoggedIn() {
        activeUsers.incrementAndGet();
    }

    public void userLoggedOut() {
        activeUsers.decrementAndGet();
    }
}
