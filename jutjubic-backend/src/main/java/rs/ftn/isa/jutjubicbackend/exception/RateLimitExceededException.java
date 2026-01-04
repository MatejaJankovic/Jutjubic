package rs.ftn.isa.jutjubicbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {

    private final long secondsUntilReset;

    public RateLimitExceededException(String message, long secondsUntilReset) {
        super(message);
        this.secondsUntilReset = secondsUntilReset;
    }

    public long getSecondsUntilReset() {
        return secondsUntilReset;
    }
}

