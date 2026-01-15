package rs.ftn.isa.jutjubicbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SwaggerAutoOpenConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerPath;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String swaggerUrl = "http://localhost:" + serverPort + swaggerPath;
        String apiDocsUrl = "http://localhost:" + serverPort + "/v3/api-docs";


        printSwaggerBanner(swaggerUrl, apiDocsUrl);
    }

    private void printSwaggerBanner(String swaggerUrl, String apiDocsUrl) {;
        log.info("Aplikacija je spremna za korišćenje!");
        log.info("Swagger UI: {}", swaggerUrl);
    }
}

