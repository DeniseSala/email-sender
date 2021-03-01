package com.personal.project.emailsender.controller;

import com.personal.project.emailsender.dto.EmailDTO;
import com.personal.project.emailsender.kafka.producer.EmailProducer;
import io.swagger.v3.oas.annotations.Operation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailProducer emailProducer;

    @Operation(description = "Asynchronously send email")
    @PostMapping
    public ResponseEntity<?> sendEmail(@RequestBody @Valid EmailDTO emailDTO) {
        emailProducer.queueEmail(emailDTO);
        return ResponseEntity.accepted().build();
    }

}
