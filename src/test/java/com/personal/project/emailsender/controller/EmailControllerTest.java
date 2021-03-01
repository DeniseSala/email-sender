package com.personal.project.emailsender.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.project.emailsender.controller.error.ValidationError;
import com.personal.project.emailsender.dto.AttachmentDTO;
import com.personal.project.emailsender.dto.EmailDTO;
import com.personal.project.emailsender.kafka.producer.EmailProducer;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EmailController.class)
public class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailProducer emailProducer;

    @ParameterizedTest
    @MethodSource("provideValidTestData")
    public void shouldProduceAnEmailEventWhenTheRequestIsValid(EmailDTO testEmailDTO) throws Exception {
        //when
        MockHttpServletResponse response = postEmailSendRequest(testEmailDTO);

        //then
        //the request is accepted
        assertEquals(ACCEPTED.value(), response.getStatus());

        //the event is produced
        verify(emailProducer).queueEmail(testEmailDTO);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidTestData")
    public void shouldFailAndDontProduceAnEventWhenTheRequestBodyIsInvalid(EmailDTO testEmailDTO, List<String> expectedErrors) throws Exception {
        //when
        MockHttpServletResponse response = postEmailSendRequest(testEmailDTO);

        //then
        //the requests is invalid
        assertEquals(BAD_REQUEST.value(), response.getStatus());
        List<String> actualErrors = getValidationErrors(response);
        assertNotNull(actualErrors);
        assertThat(actualErrors).hasSameElementsAs(expectedErrors).hasSize(expectedErrors.size());

        //the event is not produced
        verify(emailProducer, never()).queueEmail(testEmailDTO);
    }

    private List<String> getValidationErrors(MockHttpServletResponse response) throws IOException {
        return objectMapper.readValue(response.getContentAsString(), ValidationError.class).getValidationErrors();
    }

    private static Stream<Arguments> provideInvalidTestData() {
        return Stream.of(
            Arguments.of(
                new EmailDTO(null, "receiver@test.com", "subject", "body", null),
                List.of("from: must not be blank")
            ),
            Arguments.of(
                new EmailDTO(" ", "receiver@test.com", "subject", "body", null),
                List.of("from: must not be blank", "from: must be a well-formed email address")
            ),
            Arguments.of(
                new EmailDTO("invalidEmail", "receiver@test.com", "subject", "body", null),
                List.of("from: must be a well-formed email address")
            ),
            Arguments.of(
                new EmailDTO("sender@test.com", null, "subject", "body", null),
                List.of("to: must not be blank")
            ),
            Arguments.of(
                new EmailDTO("sender@test.com", " ", "subject", "body", null),
                List.of("to: must not be blank", "to: must be a well-formed email address")
            ),
            Arguments.of(
                new EmailDTO("sender@test.com", "invalidEmail", "subject", "body", null),
                List.of("to: must be a well-formed email address")
            ),
            Arguments.of(
                new EmailDTO("sender@test.com", "receiver@test.com", null, "body", null),
                List.of("subject: must not be blank")
            ),
            Arguments.of(
                new EmailDTO("sender@test.com", "receiver@test.com", " ", "body", null),
                List.of("subject: must not be blank")
            ),
            Arguments.of(
                new EmailDTO("sender@test.com", "receiver@test.com", "subject", null, null),
                List.of("body: must not be blank")
            ),
            Arguments.of(
                new EmailDTO("sender@test.com", "receiver@test.com", "subject", " ", null),
                List.of("body: must not be blank")
            ),
            Arguments.of(
                new EmailDTO("sender@test.com", "receiver@test.com", "subject", "body",
                    new AttachmentDTO(" ", "file:valid.txt")),
                List.of("attachment.name: must not be blank")
            ),
            Arguments.of(
                new EmailDTO("sender@test.com", "receiver@test.com", "subject", "body",
                    new AttachmentDTO("valid name", "not a url")),
                List.of("attachment.url: must be a valid URL")
            ),
            Arguments.of(
                new EmailDTO("sender@test.com", "receiver@test.com", "subject", "body",
                    new AttachmentDTO("valid name", null)),
                List.of("attachment.url: must not be blank")
            ),
            Arguments.of(
                new EmailDTO("sender@test.com", "receiver@test.com", "subject", "body",
                    new AttachmentDTO("valid name", " ")),
                List.of("attachment.url: must not be blank", "attachment.url: must be a valid URL")
            )
        );
    }

    private static Stream<EmailDTO> provideValidTestData() {
        return Stream.of(
            new EmailDTO("sender@test.com", "receiver@test.com", "subject", "body", null),
            new EmailDTO("sender@test.com", "receiver@test.com", "subject", "body",
                new AttachmentDTO("test attachment", "https://test.server.com/attachment")),
            new EmailDTO("sender@test.com", "receiver@test.com", "subject", "body",
                new AttachmentDTO("test attachment", "file:test_file.txt"))
        );
    }

    private MockHttpServletResponse postEmailSendRequest(EmailDTO emailDTO) throws Exception {
        String emailRequest = objectMapper.writeValueAsString(emailDTO);
        return mockMvc.perform(
            post("/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emailRequest)
        ).andReturn().getResponse();
    }
}
