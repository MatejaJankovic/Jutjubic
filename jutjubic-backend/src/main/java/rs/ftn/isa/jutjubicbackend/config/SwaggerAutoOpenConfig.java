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

    private void printSwaggerBanner(String swaggerUrl, String apiDocsUrl) {
        String banner = """
                
                ╔════════════════════════════════════════════════════════════════════════════╗
                ║                                                                            ║
                ║  🎬  JUTJUBIĆ BACKEND - USPEŠNO POKRENUT!  🎬                             ║
                ║                                                                            ║
                ╠════════════════════════════════════════════════════════════════════════════╣
                ║                                                                            ║
                ║  📚 Swagger UI:                                                            ║
                ║     %s                                  ║
                ║                                                                            ║
                ║  📄 API Docs (JSON):                                                       ║
                ║     %s                                      ║
                ║                                                                            ║
                ║  🔌 Backend API:                                                           ║
                ║     http://localhost:%s                                             ║
                ║                                                                            ║
                ╠════════════════════════════════════════════════════════════════════════════╣
                ║                                                                            ║
                ║  💡 Saveti:                                                                ║
                ║     • Kopiraj Swagger UI link i otvori u browser-u                        ║
                ║     • Koristi "Authorize" dugme za JWT autentifikaciju                    ║
                ║     • Testiraj endpoint-e direktno iz Swagger UI-a                        ║
                ║                                                                            ║
                ╚════════════════════════════════════════════════════════════════════════════╝
                
                """.formatted(swaggerUrl, apiDocsUrl, serverPort);

        System.out.println(banner);
        log.info("🚀 Aplikacija je spremna za korišćenje!");
        log.info("📖 Swagger UI: {}", swaggerUrl);
    }
}

