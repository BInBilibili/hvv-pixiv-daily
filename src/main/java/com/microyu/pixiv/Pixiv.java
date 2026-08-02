package com.microyu.pixiv;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.text.Normalizer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Pixiv {

    private static final String RANKING_API =
        "https://www.pixiv.net/ranking.php?format=json&mode=daily&p=";
    private static final int MAX_RANKING_PAGES = 10;
    private static final int RESULTS_PER_TOPIC = 10;
    private static final Path IMAGE_DIRECTORY = Paths.get("daily-images");
    private static final String IMAGE_REPOSITORY = System.getenv().getOrDefault(
        "GITHUB_REPOSITORY", "BInBilibili/hvv-pixiv-daily"
    );
    private static final String IMAGE_BRANCH = "daily-images";

    private static final List<Topic> TOPICS = Arrays.asList(
        new Topic("ウマ娘 プリティーダービー", "ウマ娘プリティーダービー"),
        new Topic("鳴潮", "鳴潮"),
        new Topic("学園アイドルマスター", "学園アイドルマスター"),
        new Topic("arknights", "arknights", "アークナイツ", "明日方舟")
    );

    public static void main(String[] args) throws IOException {
        Files.createDirectories(IMAGE_DIRECTORY);
        Map<Topic, List<Image>> results = new LinkedHashMap<>();
        for (Topic topic : TOPICS) {
            results.put(topic, new ArrayList<>());
        }

        for (int page = 1; page <= MAX_RANKING_PAGES && needsMoreResults(results); page++) {
            JSONObject response = JSON.parseObject(HttpUtls.getHttpContent(RANKING_API + page));
            JSONArray contents = response.getJSONArray("contents");
            if (contents == null || contents.isEmpty()) {
                break;
            }

            for (int i = 0; i < contents.size(); i++) {
                JSONObject artwork = contents.getJSONObject(i);
                for (Topic topic : TOPICS) {
                    List<Image> topicImages = results.get(topic);
                    if (topicImages.size() < RESULTS_PER_TOPIC && topic.matches(artwork.getJSONArray("tags"))) {
                        topicImages.add(Image.fromRankingEntry(artwork, topicImages.size() + 1));
                    }
                }
            }
        }

        for (Map.Entry<Topic, List<Image>> entry : results.entrySet()) {
            Path topicDirectory = IMAGE_DIRECTORY.resolve(entry.getKey().getDirectoryName());
            for (Image image : entry.getValue()) {
                image.downloadOriginal(topicDirectory, IMAGE_REPOSITORY, IMAGE_BRANCH);
            }
        }

        FileUtils.writeReadme(results);
    }

    private static boolean needsMoreResults(Map<Topic, List<Image>> results) {
        for (List<Image> images : results.values()) {
            if (images.size() < RESULTS_PER_TOPIC) {
                return true;
            }
        }
        return false;
    }

    static String normalizeTag(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC).replaceAll("\\s+", "");
    }

    static final class Topic {
        private final String displayName;
        private final String directoryName;
        private final List<String> normalizedTags;

        Topic(String displayName, String... tags) {
            this.displayName = displayName;
            this.directoryName = displayName;
            this.normalizedTags = new ArrayList<>();
            for (String tag : tags) {
                this.normalizedTags.add(normalizeTag(tag));
            }
        }

        String getDisplayName() {
            return displayName;
        }

        String getDirectoryName() {
            return directoryName;
        }

        boolean matches(JSONArray tags) {
            if (tags == null) {
                return false;
            }
            for (int i = 0; i < tags.size(); i++) {
                Object tag = tags.get(i);
                String tagName = tag instanceof JSONObject
                    ? ((JSONObject) tag).getString("tag")
                    : String.valueOf(tag);
                if (normalizedTags.contains(normalizeTag(tagName))) {
                    return true;
                }
            }
            return false;
        }
    }
}
