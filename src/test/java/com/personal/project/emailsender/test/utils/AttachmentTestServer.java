package com.personal.project.emailsender.test.utils;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Utility class to mock a remote server used to download attachments.
 */
@RestController
public class AttachmentTestServer {

    public static final String TEST_MEDIA_TYPE = MediaType.IMAGE_JPEG_VALUE;
    public static final String TEST_ATTACHMENT_ENDPOINT = "/get-attachment";

    @Value("classpath:spring.jpg")
    private Resource testAttachment;

    @GetMapping(
        value = TEST_ATTACHMENT_ENDPOINT,
        produces = MediaType.IMAGE_JPEG_VALUE
    )
    public @ResponseBody byte[] getTestAttachment() throws IOException {
        return testAttachment.getInputStream().readAllBytes();
    }
}
