package com.personal.project.emailsender.kafka.config;

import com.personal.project.emailsender.dto.EmailDTO;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, EmailDTO> kafkaListenerContainerFactory(
        @Value("${email-sender.kafka.listener.backoff.interval.ms}") long listenerBackoffIntervalMs,
        @Value("${email-sender.kafka.listener.retry.attempts}") long listenerRetryAttempts,
        ConsumerFactory<String, EmailDTO> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, EmailDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(AckMode.RECORD);
        factory.setErrorHandler(
            new SeekToCurrentErrorHandler(new FixedBackOff(listenerBackoffIntervalMs, listenerRetryAttempts)));
        return factory;
    }

    @Bean
    public ConsumerFactory<String, EmailDTO> consumerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(),
            new ErrorHandlingDeserializer<>(new JsonDeserializer<>(EmailDTO.class, false)));
    }
}
