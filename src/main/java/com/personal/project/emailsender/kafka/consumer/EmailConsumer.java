package com.personal.project.emailsender.kafka.consumer;

import com.personal.project.emailsender.dto.EmailDTO;
import com.personal.project.emailsender.service.EmailSenderService;
import java.io.IOException;
import javax.mail.MessagingException;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Component
@Validated
@RequiredArgsConstructor
public class EmailConsumer {

    private final EmailSenderService emailSenderService;

    @KafkaListener(topics = "${email-sender.kafka.topic.emails}")
    public void onMessage(@Valid @Payload EmailDTO emailDTO) throws MessagingException, IOException {
        log.debug("Processing email event: {}", emailDTO);
        emailSenderService.sendEmail(emailDTO);
    }
}
