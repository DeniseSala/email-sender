package com.personal.project.emailsender;

import static com.personal.project.emailsender.test.utils.AttachmentTestServer.TEST_ATTACHMENT_ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.personal.project.emailsender.dto.AttachmentDTO;
import com.personal.project.emailsender.dto.EmailDTO;
import com.personal.project.emailsender.kafka.consumer.EmailConsumer;
import com.personal.project.emailsender.test.utils.AttachmentTestServer;
import java.util.List;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeMessage;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@DirtiesContext
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {EmailSenderApplication.class,
    AttachmentTestServer.class})
@EmbeddedKafka(partitions = 1, topics = "${email-sender.kafka.topic.emails}")
public class EmailSenderIntegrationTest {

    private static final long CONSUME_EVENTS_TIMEOUT_MS = 5000L;
    private static final long RECEIVE_EMAIL_TIMEOUT_MS = 5000L;

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @LocalServerPort
    private int serverPort;

    @Value("${email-sender.kafka.listener.retry.attempts}")
    private int listenerRetryAttempts;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AttachmentTestServer attachmentTestServer;

    @SpyBean
    private EmailConsumer emailConsumer;

    @Captor
    ArgumentCaptor<EmailDTO> emailDTOArgumentCaptor;

    @Test
    public void shouldSendEmailWhenSendEmailEventIsReceived() throws Exception {
        //given
        EmailDTO testEmailDTO = createTestEmailDTO(null);

        //when
        MockHttpServletResponse response = postEmailSendRequest(testEmailDTO);

        //then
        //the request has been accepted
        assertEquals(ACCEPTED.value(), response.getStatus());

        //the event has been consumed
        verify(emailConsumer, timeout(CONSUME_EVENTS_TIMEOUT_MS)).onMessage(emailDTOArgumentCaptor.capture());
        EmailDTO consumedEmailDTO = emailDTOArgumentCaptor.getValue();
        assertEquals(testEmailDTO, consumedEmailDTO);

        //an email has been received
        greenMail.waitForIncomingEmail(RECEIVE_EMAIL_TIMEOUT_MS, 1);
        MimeMessage[] receivedEmails = greenMail.getReceivedMessages();
        assertEquals(1, receivedEmails.length);
        MimeMessage receivedEmail = receivedEmails[0];

        //from field is correct
        Address[] actualSenders = receivedEmail.getFrom();
        assertEquals(1, actualSenders.length);
        assertEquals(testEmailDTO.getFrom(), actualSenders[0].toString());

        //to field is correct
        Address[] actualRecipients = receivedEmail.getRecipients(RecipientType.TO);
        assertEquals(1, actualRecipients.length);
        assertEquals(testEmailDTO.getTo(), actualRecipients[0].toString());

        //subject field is correct
        assertEquals(testEmailDTO.getSubject(), receivedEmail.getSubject());

        //body content is correct
        MimeMessageParser emailParser = new MimeMessageParser(receivedEmail).parse();
        assertEquals(testEmailDTO.getBody().trim(), emailParser.getPlainContent().trim());

        //the attachment is correct
        Assertions.assertFalse(emailParser.hasAttachments());
    }

    @Test
    public void shouldSendEmailWithAttachmentWhenSendEmailEventIsReceived() throws Exception {
        //given
        String attachmentUrl = "http://localhost:" + serverPort + TEST_ATTACHMENT_ENDPOINT;
        AttachmentDTO testAttachmentDTO = new AttachmentDTO("attachment_name", attachmentUrl);
        EmailDTO testEmailDTO = createTestEmailDTO(testAttachmentDTO);

        //when
        MockHttpServletResponse response = postEmailSendRequest(testEmailDTO);

        //then
        //the request has been accepted
        assertEquals(ACCEPTED.value(), response.getStatus());

        //the event has been consumed
        verify(emailConsumer, timeout(CONSUME_EVENTS_TIMEOUT_MS)).onMessage(emailDTOArgumentCaptor.capture());
        EmailDTO consumedEmailDTO = emailDTOArgumentCaptor.getValue();
        assertEquals(testEmailDTO, consumedEmailDTO);

        //an email has been received
        greenMail.waitForIncomingEmail(RECEIVE_EMAIL_TIMEOUT_MS, 1);
        MimeMessage[] receivedEmails = greenMail.getReceivedMessages();
        assertEquals(1, receivedEmails.length);
        MimeMessage receivedEmail = receivedEmails[0];

        //from field is correct
        Address[] actualSenders = receivedEmail.getFrom();
        assertEquals(1, actualSenders.length);
        assertEquals(testEmailDTO.getFrom(), actualSenders[0].toString());

        //to field is correct
        Address[] actualRecipients = receivedEmail.getRecipients(RecipientType.TO);
        assertEquals(1, actualRecipients.length);
        assertEquals(testEmailDTO.getTo(), actualRecipients[0].toString());

        //subject field is correct
        assertEquals(testEmailDTO.getSubject(), receivedEmail.getSubject());

        //body content is correct
        MimeMessageParser emailParser = new MimeMessageParser(receivedEmail).parse();
        assertEquals(testEmailDTO.getBody().trim(), emailParser.getPlainContent().trim());

        //the attachment is correct
        assertTrue(emailParser.hasAttachments());
        List<DataSource> actualAttachments = emailParser.getAttachmentList();
        assertEquals(1, actualAttachments.size());
        DataSource actualAttachment = actualAttachments.get(0);
        assertEquals(AttachmentTestServer.TEST_MEDIA_TYPE, actualAttachment.getContentType());
        byte[] actualAttachmentBytes = actualAttachment.getInputStream().readAllBytes();
        assertArrayEquals(attachmentTestServer.getTestAttachment(), actualAttachmentBytes);
    }

    @Test
    public void shouldRetryWhenTheAttachmentCannotBeDownloaded() throws Exception {
        //given
        String nonExistingAttachmentUrl = "http://localhost:" + serverPort + "/non-existing";
        AttachmentDTO testAttachmentDTO = new AttachmentDTO("attachment_name", nonExistingAttachmentUrl);
        EmailDTO testEmailDTO = createTestEmailDTO(testAttachmentDTO);

        //when
        MockHttpServletResponse response = postEmailSendRequest(testEmailDTO);

        //then
        //the request is accepted
        assertEquals(ACCEPTED.value(), response.getStatus());

        //retries to send the email
        int expectedNumberOfConsumedEvents = listenerRetryAttempts + 1;
        verify(emailConsumer, timeout(CONSUME_EVENTS_TIMEOUT_MS).times(expectedNumberOfConsumedEvents))
            .onMessage(emailDTOArgumentCaptor.capture());
        EmailDTO actualEmailDTO = emailDTOArgumentCaptor.getValue();
        assertEquals(testEmailDTO, actualEmailDTO);
    }

    private MockHttpServletResponse postEmailSendRequest(EmailDTO emailDTO) throws Exception {
        String testEmailRequest = objectMapper.writeValueAsString(emailDTO);
        return mockMvc.perform(
            post("/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testEmailRequest)
        ).andReturn().getResponse();
    }

    private static EmailDTO createTestEmailDTO(AttachmentDTO attachment) {
        return new EmailDTO("from@email.com", "to@email.com", "subj", "text", attachment);
    }
}
