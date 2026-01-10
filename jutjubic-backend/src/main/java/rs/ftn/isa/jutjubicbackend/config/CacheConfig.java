// File: `jutjubic-backend/src/main/java/rs/ftn/isa/jutjubicbackend/config/CacheConfig.java`
package rs.ftn.isa.jutjubicbackend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .recordStats() // enables metrics (resolves the WARN)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(5_000);
    }

    @Bean
    public CaffeineCacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager manager = new CaffeineCacheManager("comments", "videos", "users");
        manager.setCaffeine(caffeine);
        return manager;
    }
}
