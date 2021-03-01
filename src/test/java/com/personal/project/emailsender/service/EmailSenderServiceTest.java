package com.personal.project.emailsender.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personal.project.emailsender.dto.AttachmentDTO;
import com.personal.project.emailsender.dto.EmailDTO;
import com.personal.project.emailsender.service.UrlDownloader.DownloadedContent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.activation.DataSource;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
public class EmailSenderServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UrlDownloader urlDownloader;

    @Captor
    private ArgumentCaptor<MimeMessage> mimeMessageArgumentCaptor;

    private EmailSenderService emailSenderService;

    @BeforeEach
    public void init() {
        emailSenderService = new EmailSenderService(mailSender, urlDownloader);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    public void shouldConvertEmailDTOInMimeMessageCorrectly() throws Exception {
        //given
        EmailDTO testEmailDTO = new EmailDTO("from@test.com", "to@test.com", "subj", "text", null);

        //when
        emailSenderService.sendEmail(testEmailDTO);

        //then
        //the message has been sent
        verify(mailSender).send(mimeMessageArgumentCaptor.capture());
        MimeMessage capturedMimeMessage = mimeMessageArgumentCaptor.getValue();

        //from field is correct
        assertEquals(1, capturedMimeMessage.getFrom().length);
        assertEquals(testEmailDTO.getFrom(), capturedMimeMessage.getFrom()[0].toString());

        //to field is correct
        assertEquals(1, capturedMimeMessage.getAllRecipients().length);
        assertEquals(testEmailDTO.getTo(), capturedMimeMessage.getAllRecipients()[0].toString());

        //subject is correct
        assertEquals(testEmailDTO.getSubject(), capturedMimeMessage.getSubject());

        //body content is correct
        MimeMessageParser parser = new MimeMessageParser(capturedMimeMessage).parse();
        assertEquals(testEmailDTO.getBody(), parser.getPlainContent());

        //there is no attachment
        assertFalse(parser.hasAttachments());
    }

    @Test
    public void shouldConvertEmailDTOWithAttachmentInMimeMessageCorrectly() throws Exception {
        //given
        EmailDTO testEmailDTO = new EmailDTO("from@test.com", "to@test.com", "subj", "text",
            new AttachmentDTO("file name", "http://test.com/attachment"));
        DownloadedContent testDownloadedContent = new DownloadedContent(MediaType.TEXT_PLAIN_VALUE,
            "test attachment".getBytes(StandardCharsets.UTF_8));
        when(urlDownloader.downloadContent(testEmailDTO.getAttachment().getUrl())).thenReturn(testDownloadedContent);

        //when
        emailSenderService.sendEmail(testEmailDTO);

        //then
        //the message has been sent
        verify(mailSender).send(mimeMessageArgumentCaptor.capture());
        MimeMessage capturedMimeMessage = mimeMessageArgumentCaptor.getValue();

        //from field is correct
        assertEquals(1, capturedMimeMessage.getFrom().length);
        assertEquals(testEmailDTO.getFrom(), capturedMimeMessage.getFrom()[0].toString());

        //to field is correct
        assertEquals(1, capturedMimeMessage.getAllRecipients().length);
        assertEquals(testEmailDTO.getTo(), capturedMimeMessage.getAllRecipients()[0].toString());

        //subject is correct
        assertEquals(testEmailDTO.getSubject(), capturedMimeMessage.getSubject());

        //body content is correct
        MimeMessageParser parser = new MimeMessageParser(capturedMimeMessage).parse();
        assertEquals(testEmailDTO.getBody(), parser.getPlainContent());

        //the attachment is correct
        assertTrue(parser.hasAttachments());
        List<DataSource> attachmentList = parser.getAttachmentList();
        assertEquals(1, attachmentList.size());
        DataSource actualAttachment = attachmentList.get(0);
        assertEquals(testDownloadedContent.getContentType(), actualAttachment.getContentType());
        byte[] actualAttachmentBytes = actualAttachment.getInputStream().readAllBytes();
        assertArrayEquals(testDownloadedContent.getContent(), actualAttachmentBytes);
    }
}
