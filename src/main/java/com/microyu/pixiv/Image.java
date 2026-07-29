package com.microyu.pixiv;

import com.alibaba.fastjson.JSONObject;

public class Image {
    private final String title;
    private final String pageUrl;
    private final String previewUrl;
    private final String originalJpgUrl;
    private final String originalPngUrl;
    private final int topicRank;
    private final int dailyRank;

    private Image(String title, String pageUrl, String previewUrl, String originalJpgUrl,
                  String originalPngUrl, int topicRank, int dailyRank) {
        this.title = title;
        this.pageUrl = pageUrl;
        this.previewUrl = previewUrl;
        this.originalJpgUrl = originalJpgUrl;
        this.originalPngUrl = originalPngUrl;
        this.topicRank = topicRank;
        this.dailyRank = dailyRank;
    }

    static Image fromRankingEntry(JSONObject artwork, int topicRank) {
        String thumbnailUrl = artwork.getString("url");
        String proxiedThumbnail = thumbnailUrl.replace("i.pximg.net", "pixiv.microyu.workers.dev");
        String original = proxiedThumbnail
            .replaceFirst("/c/\\d+x\\d+/img-master/", "/img-original/")
            .replace("_master1200", "");
        int extensionIndex = original.lastIndexOf('.');
        String originalBase = extensionIndex >= 0 ? original.substring(0, extensionIndex) : original;

        return new Image(
            artwork.getString("title"),
            "https://www.pixiv.net/artworks/" + artwork.getString("illust_id"),
            proxiedThumbnail,
            originalBase + ".jpg",
            originalBase + ".png",
            topicRank,
            artwork.getIntValue("rank")
        );
    }

    String toMarkdown() {
        String safeTitle = title == null ? "Untitled" : title.replace("|", "\\|").replace("\n", " ");
        return String.format(
            "![](%s) **#%d** [%s](%s)<br>综合日榜 #%d · [JPG](%s) [PNG](%s)",
            previewUrl, topicRank, safeTitle, pageUrl, dailyRank, originalJpgUrl, originalPngUrl
        );
    }
}
