package com.microyu.pixiv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public class FileUtils {

    private static final Path README_PATH = Paths.get("README.md");
    private static final ZoneId PIXIV_TIME_ZONE = ZoneId.of("Asia/Tokyo");

    public static void writeReadme(Map<Pixiv.Topic, List<Image>> results) throws IOException {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# hvv-pixiv-daily\n\n")
            .append("Update: ")
            .append(LocalDate.now(PIXIV_TIME_ZONE))
            .append(" (Pixiv daily ranking)\n\n");

        for (Map.Entry<Pixiv.Topic, List<Image>> entry : results.entrySet()) {
            markdown.append("## ").append(entry.getKey().getDisplayName()).append(" 日榜 Top 10\n\n");
            List<Image> images = entry.getValue();
            if (images.isEmpty()) {
                markdown.append("当天 Pixiv 综合日榜中没有匹配该标签的作品。\n\n");
                continue;
            }

            markdown.append("<details>\n")
                .append("<summary>展开榜单（")
                .append(images.size())
                .append(" 幅）</summary>\n\n")
                .append("|  |  |\n| :---: | :---: |\n");
            for (int i = 0; i < images.size(); i++) {
                if (i % 2 == 0) {
                    markdown.append('|');
                }
                markdown.append(images.get(i).toMarkdown()).append('|');
                if (i % 2 == 1) {
                    markdown.append('\n');
                }
            }
            if (images.size() % 2 == 1) {
                markdown.append(" |\n");
            }
            markdown.append("\n</details>\n\n");
        }

        markdown.append("榜单来源：Pixiv 官方综合日榜；每组按综合日榜名次筛选指定标签，最多展示 10 幅。\n");
        Files.write(README_PATH, markdown.toString().getBytes(StandardCharsets.UTF_8));
    }
}
