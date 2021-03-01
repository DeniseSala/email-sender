package com.personal.project.emailsender.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UrlDownloaderImpl implements UrlDownloader{

    @Override
    public DownloadedContent downloadContent(String url) throws IOException {
        log.debug("Downloading attachment from URL: {}", url);
        URLConnection urlConnection = new URL(url).openConnection();
        try (InputStream inputStream = urlConnection.getInputStream()) {
            return new DownloadedContent(urlConnection.getContentType(), inputStream.readAllBytes());
        }
    }
}
