package com.personal.project.emailsender.service;

import com.personal.project.emailsender.dto.EmailDTO;
import com.personal.project.emailsender.service.UrlDownloader.DownloadedContent;
import java.io.IOException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderService {

    private final JavaMailSender javaMailSender;

    private final UrlDownloader urlDownloader;

    public void sendEmail(EmailDTO emailDTO) throws IOException, MessagingException {
        MimeMessage mimeMessage = toMailMessage(emailDTO);
        javaMailSender.send(mimeMessage);
    }

    private MimeMessage toMailMessage(EmailDTO emailDTO) throws MessagingException, IOException {
        MimeMessage mailMessage = javaMailSender.createMimeMessage();

        boolean hasAttachment = emailDTO.getAttachment() != null;
        MimeMessageHelper helper = new MimeMessageHelper(mailMessage, hasAttachment);
        helper.setFrom(emailDTO.getFrom());
        helper.setTo(emailDTO.getTo());
        helper.setSubject(emailDTO.getSubject());
        helper.setText(emailDTO.getBody());

        if (hasAttachment) {
            DownloadedContent downloadedContent = urlDownloader.downloadContent(emailDTO.getAttachment().getUrl());
            ByteArrayResource attachmentResource = new ByteArrayResource(downloadedContent.getContent());
            String attachmentName = emailDTO.getAttachment().getName();
            String attachmentContentType = downloadedContent.getContentType();
            log.debug("Adding attachment with name {}, content type {}, size {}", attachmentName, attachmentContentType,
                attachmentResource.contentLength());
            helper.addAttachment(attachmentName, attachmentResource, attachmentContentType);
        }
        return mailMessage;
    }
}
