package world.jerry.feedhub.api.infrastructure.opml;

import com.rometools.opml.feed.opml.Opml;
import com.rometools.opml.feed.opml.Outline;
import com.rometools.rome.io.WireFeedInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class OpmlParser {

    public List<ParsedOpmlEntry> parse(InputStream inputStream) {
        try {
            WireFeedInput input = new WireFeedInput();
            Opml opml = (Opml) input.build(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            List<ParsedOpmlEntry> entries = new ArrayList<>();
            extractOutlines(opml.getOutlines(), entries);

            log.info("OPML 파싱 완료: {}개 피드 발견", entries.size());
            return entries;
        } catch (Exception e) {
            log.error("OPML 파싱 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void extractOutlines(List<Outline> outlines, List<ParsedOpmlEntry> result) {
        if (outlines == null) {
            return;
        }

        for (Outline outline : outlines) {
            if (outline.getXmlUrl() != null && !outline.getXmlUrl().isBlank()) {
                String title = outline.getTitle();
                if (title == null || title.isBlank()) {
                    title = outline.getText();
                }
                if (title == null || title.isBlank()) {
                    title = outline.getXmlUrl();
                }

                result.add(new ParsedOpmlEntry(
                        title,
                        outline.getXmlUrl(),
                        outline.getHtmlUrl()
                ));
            }

            // 재귀적으로 하위 outline 처리 (폴더 구조)
            if (outline.getChildren() != null && !outline.getChildren().isEmpty()) {
                extractOutlines(outline.getChildren(), result);
            }
        }
    }
}
