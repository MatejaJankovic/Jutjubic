package rs.ftn.isa.jutjubicbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rs.ftn.isa.jutjubicbackend.model.UploadEvent;
import rs.ftn.isa.jutjubicbackend.service.UploadEventJsonProducer;
import rs.ftn.isa.jutjubicbackend.service.UploadEventProtobufProducer;


@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestController {

    @Autowired
    private UploadEventJsonProducer jsonProducer;

    @Autowired
    private UploadEventProtobufProducer protobufProducer;

    @GetMapping("/send-batch")
    public String sendBatch(@RequestParam(defaultValue = "50") int count) {

        System.out.println("\n========== POČETAK TESTA - " + count + " PORUKA ==========\n");

        for (int i = 1; i <= count; i++) {
            UploadEvent event = new UploadEvent(
                    "Test Video " + i,
                    "author-" + i,
                    (long) (Math.random() * 50000000 + 5000000),
                    System.currentTimeMillis()
            );

            jsonProducer.sendJsonEvent(event);
            protobufProducer.sendProtobufEvent(event);
        }

        System.out.println("\n========== KRAJ TESTA ==========\n");

        return "Poslato " + count + " poruka u oba formata! Proveri logove.";
    }
}


