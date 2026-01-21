package world.jerry.feedhub.api.application.rss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.application.rss.dto.OpmlImportResult;
import world.jerry.feedhub.api.application.rss.dto.RegisterRssInfoCommand;
import world.jerry.feedhub.api.application.rss.dto.RssInfoDetail;
import world.jerry.feedhub.api.domain.rss.RssInfoRepository;
import world.jerry.feedhub.api.infrastructure.opml.OpmlParser;
import world.jerry.feedhub.api.infrastructure.opml.ParsedOpmlEntry;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpmlImportService {

    private final OpmlParser opmlParser;
    private final RssInfoRepository rssInfoRepository;
    private final RssInfoService rssInfoService;
    private final CollectorCommandService collectorCommandService;

    @Transactional
    public OpmlImportResult importOpml(InputStream opmlStream, boolean syncAfterImport) {
        List<ParsedOpmlEntry> entries = opmlParser.parse(opmlStream);

        int imported = 0;
        List<String> skippedUrls = new ArrayList<>();
        List<Long> syncRequestedIds = new ArrayList<>();

        for (ParsedOpmlEntry entry : entries) {
            if (rssInfoRepository.existsByRssUrl(entry.xmlUrl())) {
                log.debug("RSS URL 이미 존재, 건너뜀: {}", entry.xmlUrl());
                skippedUrls.add(entry.xmlUrl());
                continue;
            }

            try {
                RssInfoDetail registered = rssInfoService.registerRssInfo(
                        new RegisterRssInfoCommand(
                                entry.title(),
                                null,
                                entry.xmlUrl(),
                                entry.htmlUrl(),
                                null,  // crawlUrl
                                null,  // language
                                null   // blogType 자동 감지
                        )
                );
                imported++;
                syncRequestedIds.add(registered.id());
                log.info("RSS 소스 등록 완료: {} ({})", entry.title(), entry.xmlUrl());

            } catch (Exception e) {
                log.error("RSS 소스 등록 실패: {} - {}", entry.xmlUrl(), e.getMessage());
                skippedUrls.add(entry.xmlUrl());
            }
        }

        log.info("OPML 임포트 완료: 총 {}개 중 {}개 등록, {}개 건너뜀",
                entries.size(), imported, skippedUrls.size());

        return new OpmlImportResult(entries.size(), imported, skippedUrls, syncRequestedIds);
    }
}
