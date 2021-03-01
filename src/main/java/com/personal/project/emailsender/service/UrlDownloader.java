package com.personal.project.emailsender.service;

import java.io.IOException;
import lombok.Value;

public interface UrlDownloader {

    DownloadedContent downloadContent(String url) throws IOException;

    @Value
    class DownloadedContent {
        String contentType;
        byte[] content;
    }
}
