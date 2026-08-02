package com.microyu.pixiv;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class HttpUtls {

    private static final int TIMEOUT_MILLIS = 30_000;
    private static final int MAX_IMAGE_BYTES = 25 * 1024 * 1024;

    public static String getHttpContent(String url) throws IOException {
        HttpURLConnection connection = openConnection(url);

        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Pixiv returned HTTP " + status + " for " + url);
            }
            try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }
                return output.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            connection.disconnect();
        }
    }

    public static boolean downloadImage(String url, Path destination) throws IOException {
        HttpURLConnection connection = openConnection(url);
        Path temporaryFile = destination.resolveSibling(destination.getFileName() + ".tmp");

        try {
            int status = connection.getResponseCode();
            String contentType = connection.getContentType();
            int contentLength = connection.getContentLength();
            if (status < 200 || status >= 300 || contentType == null || !contentType.startsWith("image/")) {
                return false;
            }
            if (contentLength > MAX_IMAGE_BYTES) {
                return false;
            }

            Files.createDirectories(destination.getParent());
            try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                 java.io.OutputStream output = Files.newOutputStream(temporaryFile)) {
                byte[] buffer = new byte[8192];
                int length;
                int totalBytes = 0;
                while ((length = input.read(buffer)) != -1) {
                    totalBytes += length;
                    if (totalBytes > MAX_IMAGE_BYTES) {
                        Files.deleteIfExists(temporaryFile);
                        return false;
                    }
                    output.write(buffer, 0, length);
                }
            }
            Files.move(temporaryFile, destination, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } finally {
            Files.deleteIfExists(temporaryFile);
            connection.disconnect();
        }
    }

    private static HttpURLConnection openConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Referer", "https://www.pixiv.net/");
        return connection;
    }
}
