package com.personal.project.emailsender.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.personal.project.emailsender.dto.EmailDTO;
import com.personal.project.emailsender.kafka.config.KafkaConfig;
import com.personal.project.emailsender.service.EmailSenderService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.mail.MailSendException;
import org.springframework.test.annotation.DirtiesContext;

@EnableAutoConfiguration
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {KafkaConfig.class})
@EmbeddedKafka(partitions = 1, topics = "${email-sender.kafka.topic.emails}")
public class EmailConsumerTest {

    private static final long CONSUME_EVENTS_TIMEOUT_MS = 2000L;

    @Value("${email-sender.kafka.listener.retry.attempts}")
    private int listenerRetryAttempts;

    @Value("${email-sender.kafka.topic.emails}")
    private String emailTopic;

    @MockBean
    private EmailSenderService emailSenderService;

    @Autowired
    private KafkaTemplate<String, EmailDTO> testProducer;

    @Autowired
    private KafkaTemplate<String, String> invalidEventProducer;

    @SpyBean
    private EmailConsumer emailConsumer;

    @Test
    public void shouldRetryWhenTheEmailCannotBeSent() throws Exception {
        //given
        EmailDTO emailDTO = createTestEmailDTO("subj");
        doThrow(new MailSendException("test error message")).when(emailSenderService).sendEmail(emailDTO);

        //when
        testProducer.send(emailTopic, emailDTO);

        //then
        int expectedNumberOfConsumedEvents = getExpectedNumberOfConsumedEventsOnRetry();
        verify(emailSenderService, timeout(CONSUME_EVENTS_TIMEOUT_MS).times(expectedNumberOfConsumedEvents)).sendEmail(emailDTO);
    }

    @Test
    public void shouldFailWithoutRetryingWhenEmailDTOCannotBeDeserialized() throws Exception {
        //when
        invalidEventProducer.send(emailTopic, "invalidMessage");

        //then
        verify(emailConsumer, after(CONSUME_EVENTS_TIMEOUT_MS).never()).onMessage(any());
    }

    @Test
    public void shouldSendEmailWhenEmailDTOIsValid() throws Exception {
        //given
        EmailDTO emailDTO = createTestEmailDTO("subj");

        //when
        testProducer.send(emailTopic, emailDTO);

        //then
        verify(emailSenderService, timeout(CONSUME_EVENTS_TIMEOUT_MS)).sendEmail(emailDTO);
    }

    @Test
    public void shouldConsumeTheSecondMessageOnlyAfterTheFirstHasBeenRetried() throws Exception {
        //given
        EmailDTO emailDTO1 = createTestEmailDTO("failing");
        EmailDTO emailDTO2 = createTestEmailDTO("successful");
        //the first email cannot be sent
        doThrow(new MailSendException("test error")).when(emailSenderService).sendEmail(emailDTO1);

        //when
        testProducer.send(emailTopic, emailDTO1);
        testProducer.send(emailTopic, emailDTO2);

        //then
        InOrder order = Mockito.inOrder(emailConsumer);
        int expectedNumberOfConsumedEvents = getExpectedNumberOfConsumedEventsOnRetry();
        order.verify(emailConsumer, timeout(CONSUME_EVENTS_TIMEOUT_MS).times(expectedNumberOfConsumedEvents))
            .onMessage(emailDTO1);
        order.verify(emailConsumer, timeout(CONSUME_EVENTS_TIMEOUT_MS)).onMessage(emailDTO2);
    }

    private int getExpectedNumberOfConsumedEventsOnRetry() {
        return listenerRetryAttempts + 1;
    }

    private static EmailDTO createTestEmailDTO(String subject) {
        return new EmailDTO("from@email.com", "to@email.com", subject, "text", null);
    }

    @TestConfiguration
    static class InvalidEventProducerConfig {

        @Bean
        public ProducerFactory<String, String> invalidEventProducerFactory(EmbeddedKafkaBroker embeddedKafka) {
            return new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(embeddedKafka));
        }

        @Bean
        public KafkaTemplate<String, String> invalidEventProducer(
            ProducerFactory<String, String> invalidEventProducerFactory) {
            return new KafkaTemplate<>(invalidEventProducerFactory);
        }
    }

}
