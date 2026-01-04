package rs.ftn.isa.jutjubicbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class JutjubicBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(JutjubicBackendApplication.class, args);
    }

}
