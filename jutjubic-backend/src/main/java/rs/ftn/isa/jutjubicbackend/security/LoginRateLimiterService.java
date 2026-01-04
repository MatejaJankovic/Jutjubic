package rs.ftn.isa.jutjubicbackend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {

    @Value("${rate.limit.login.max-attempts}")
    private int maxAttempts;

    @Value("${rate.limit.login.window-minutes}")
    private int windowMinutes;

    private final Map<String, LoginAttempts> attemptsCache = new ConcurrentHashMap<>();

    public boolean isBlocked(String ipAddress) {
        LoginAttempts attempts = attemptsCache.get(ipAddress);

        if (attempts == null) {
            return false;
        }

        // Reset if window has passed
        if (attempts.getWindowStart().plusMinutes(windowMinutes).isBefore(LocalDateTime.now())) {
            attemptsCache.remove(ipAddress);
            return false;
        }

        return attempts.getCount() >= maxAttempts;
    }

    public void recordFailedAttempt(String ipAddress) {
        LoginAttempts attempts = attemptsCache.get(ipAddress);

        if (attempts == null || attempts.getWindowStart().plusMinutes(windowMinutes).isBefore(LocalDateTime.now())) {
            attemptsCache.put(ipAddress, new LoginAttempts(1, LocalDateTime.now()));
        } else {
            attempts.increment();
        }
    }

    public void resetAttempts(String ipAddress) {
        attemptsCache.remove(ipAddress);
    }

    public int getRemainingAttempts(String ipAddress) {
        LoginAttempts attempts = attemptsCache.get(ipAddress);

        if (attempts == null) {
            return maxAttempts;
        }

        if (attempts.getWindowStart().plusMinutes(windowMinutes).isBefore(LocalDateTime.now())) {
            return maxAttempts;
        }

        return Math.max(0, maxAttempts - attempts.getCount());
    }

    public long getSecondsUntilReset(String ipAddress) {
        LoginAttempts attempts = attemptsCache.get(ipAddress);

        if (attempts == null) {
            return 0;
        }

        LocalDateTime resetTime = attempts.getWindowStart().plusMinutes(windowMinutes);
        if (resetTime.isBefore(LocalDateTime.now())) {
            return 0;
        }

        return java.time.Duration.between(LocalDateTime.now(), resetTime).getSeconds();
    }

    private static class LoginAttempts {
        private int count;
        private LocalDateTime windowStart;

        public LoginAttempts(int count, LocalDateTime windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }

        public int getCount() {
            return count;
        }

        public LocalDateTime getWindowStart() {
            return windowStart;
        }

        public void increment() {
            this.count++;
        }
    }
}

