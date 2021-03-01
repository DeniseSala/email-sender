package com.personal.project.emailsender.kafka.producer;


import com.personal.project.emailsender.dto.EmailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailProducer {

    private final String emailTopic;

    private final KafkaTemplate<String, EmailDTO> kafkaTemplate;

    public EmailProducer(@Value("${email-sender.kafka.topic.emails}") String emailTopic,
        KafkaTemplate<String, EmailDTO> kafkaTemplate) {

        this.emailTopic = emailTopic;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void queueEmail(EmailDTO emailDTO) {
        log.debug("Sending email event to kafka: {}", emailDTO);
        kafkaTemplate.send(emailTopic, emailDTO);
    }
}
