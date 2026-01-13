package world.jerry.feedhub.api.infrastructure.opml;

import com.rometools.opml.feed.opml.Attribute;
import com.rometools.opml.feed.opml.Opml;
import com.rometools.opml.feed.opml.Outline;
import com.rometools.rome.io.WireFeedOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import world.jerry.feedhub.api.application.rss.dto.RssInfoDetail;

import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * OPML 문서 빌더
 * RssInfo 목록을 OPML 포맷으로 변환
 */
@Slf4j
@Component
public class OpmlBuilder {

    private static final String OPML_TITLE = "FeedHub Subscriptions";

    /**
     * RssInfo 목록을 OPML 문서로 변환
     */
    public String build(List<RssInfoDetail> rssInfos) {
        try {
            Opml opml = new Opml();
            opml.setFeedType("opml_2.0");
            opml.setTitle(OPML_TITLE);
            opml.setCreated(Date.from(Instant.now()));

            List<Outline> outlines = rssInfos.stream()
                    .map(this::toOutline)
                    .toList();

            opml.setOutlines(outlines);

            WireFeedOutput output = new WireFeedOutput();
            StringWriter writer = new StringWriter();
            output.output(opml, writer);

            log.info("OPML 내보내기 완료: {}개 피드", rssInfos.size());
            return writer.toString();
        } catch (Exception e) {
            log.error("OPML 빌드 실패: {}", e.getMessage());
            throw new RuntimeException("OPML 빌드 실패", e);
        }
    }

    private Outline toOutline(RssInfoDetail rssInfo) {
        Outline outline = new Outline();
        outline.setType("rss");
        outline.setText(rssInfo.blogName());
        outline.setTitle(rssInfo.blogName());

        // xmlUrl, htmlUrl은 Attribute로 설정
        List<Attribute> attributes = new ArrayList<>();
        if (rssInfo.rssUrl() != null) {
            attributes.add(new Attribute("xmlUrl", rssInfo.rssUrl()));
        }
        if (rssInfo.siteUrl() != null) {
            attributes.add(new Attribute("htmlUrl", rssInfo.siteUrl()));
        }
        outline.setAttributes(attributes);

        return outline;
    }
}
