package com.microyu.pixiv;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.nio.file.Path;

public class Image {
    private final String title;
    private final String pageUrl;
    private final String previewUrl;
    private final String originalJpgUrl;
    private final String originalPngUrl;
    private final String artworkId;
    private final int topicRank;
    private final int dailyRank;

    private String displayUrl;

    private Image(String title, String pageUrl, String previewUrl, String originalJpgUrl,
                  String originalPngUrl, String artworkId, int topicRank, int dailyRank) {
        this.title = title;
        this.pageUrl = pageUrl;
        this.previewUrl = previewUrl;
        this.originalJpgUrl = originalJpgUrl;
        this.originalPngUrl = originalPngUrl;
        this.artworkId = artworkId;
        this.topicRank = topicRank;
        this.dailyRank = dailyRank;
    }

    static Image fromRankingEntry(JSONObject artwork, int topicRank) {
        String thumbnailUrl = artwork.getString("url");
        String artworkId = artwork.getString("illust_id");
        String proxiedThumbnail = thumbnailUrl.replace("i.pximg.net", "pixiv.microyu.workers.dev");
        String original = proxiedThumbnail
            .replaceFirst("/c/\\d+x\\d+/img-master/", "/img-original/")
            .replace("_master1200", "");
        int extensionIndex = original.lastIndexOf('.');
        String originalBase = extensionIndex >= 0 ? original.substring(0, extensionIndex) : original;

        return new Image(
            artwork.getString("title"),
            "https://www.pixiv.net/artworks/" + artworkId,
            proxiedThumbnail,
            originalBase + ".jpg",
            originalBase + ".png",
            artworkId,
            topicRank,
            artwork.getIntValue("rank")
        );
    }

    void downloadOriginal(Path directory, String repository, String branch) {
        if (downloadOriginal(directory, repository, branch, originalJpgUrl, "jpg")) {
            return;
        }
        downloadOriginal(directory, repository, branch, originalPngUrl, "png");
    }

    private boolean downloadOriginal(Path directory, String repository, String branch, String sourceUrl,
                                     String extension) {
        String fileName = artworkId + "_p0." + extension;
        Path destination = directory.resolve(fileName);
        try {
            if (!HttpUtls.downloadImage(sourceUrl, destination)) {
                return false;
            }
            displayUrl = String.format(
                "https://raw.githubusercontent.com/%s/%s/%s/%s",
                repository, branch, directory.toString().replace('\\', '/'), fileName
            );
            return true;
        } catch (IOException exception) {
            System.err.println("Unable to download original image " + artworkId + ": " + exception.getMessage());
            return false;
        }
    }

    String toMarkdown() {
        String safeTitle = title == null ? "Untitled" : title.replace("|", "\\|").replace("\n", " ");
        return String.format(
            "![](%s) **#%d** [%s](%s)<br>综合日榜 #%d · [JPG](%s) [PNG](%s)",
            displayUrl == null ? previewUrl : displayUrl,
            topicRank, safeTitle, pageUrl, dailyRank, originalJpgUrl, originalPngUrl
        );
    }
}
