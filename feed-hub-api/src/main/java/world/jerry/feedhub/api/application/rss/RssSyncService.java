package world.jerry.feedhub.api.application.rss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.application.rss.dto.SyncResult;
import world.jerry.feedhub.api.domain.feed.FeedEntry;
import world.jerry.feedhub.api.domain.feed.FeedEntryRepository;
import world.jerry.feedhub.api.domain.rss.RssInfo;
import world.jerry.feedhub.api.domain.rss.RssInfoRepository;
import world.jerry.feedhub.api.infrastructure.rss.ParsedFeedEntry;
import world.jerry.feedhub.api.infrastructure.rss.RssParser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssSyncService {

    private final RssInfoRepository rssInfoRepository;
    private final FeedEntryRepository feedEntryRepository;
    private final RssParser rssParser;

    /**
     * 단일 RSS 소스 동기화
     */
    @Transactional
    public SyncResult syncRssSource(Long rssInfoId) {
        RssInfo rssInfo = rssInfoRepository.findById(rssInfoId)
                .orElseThrow(() -> new NoSuchElementException("RSS 소스를 찾을 수 없습니다: " + rssInfoId));

        return syncRssInfo(rssInfo);
    }

    /**
     * 전체 RSS 소스 동기화
     */
    @Transactional
    public List<SyncResult> syncAllRssSources() {
        List<RssInfo> allRssInfos = rssInfoRepository.findAll();
        List<SyncResult> results = new ArrayList<>();

        for (RssInfo rssInfo : allRssInfos) {
            try {
                SyncResult result = syncRssInfo(rssInfo);
                results.add(result);
            } catch (Exception e) {
                log.error("RSS 동기화 실패: id={}, blogName={}, error={}",
                        rssInfo.getId(), rssInfo.getBlogName(), e.getMessage());
                // 실패해도 다음 소스 계속 처리
                results.add(new SyncResult(
                        rssInfo.getId(),
                        rssInfo.getBlogName(),
                        0,
                        0,
                        rssInfo.getLastSyncAt()
                ));
            }
        }

        return results;
    }

    private SyncResult syncRssInfo(RssInfo rssInfo) {
        log.info("RSS 동기화 시작: id={}, blogName={}, url={}",
                rssInfo.getId(), rssInfo.getBlogName(), rssInfo.getRssUrl());

        List<ParsedFeedEntry> entries = rssParser.parse(rssInfo.getRssUrl());

        int syncedCount = 0;
        int skippedCount = 0;

        for (ParsedFeedEntry entry : entries) {
            if (entry.guid() == null || entry.guid().isBlank()) {
                log.warn("GUID가 없는 엔트리 건너뜀: title={}", entry.title());
                skippedCount++;
                continue;
            }

            boolean exists = feedEntryRepository.existsByRssInfoIdAndGuid(rssInfo.getId(), entry.guid());
            if (exists) {
                skippedCount++;
                continue;
            }

            FeedEntry feedEntry = new FeedEntry(
                    rssInfo.getId(),
                    entry.title(),
                    entry.link(),
                    entry.description(),
                    entry.author(),
                    entry.publishedAt(),
                    entry.guid()
            );
            feedEntryRepository.save(feedEntry);
            syncedCount++;
        }

        Instant now = Instant.now();
        rssInfo.updateLastSyncAt(now);

        log.info("RSS 동기화 완료: id={}, blogName={}, synced={}, skipped={}",
                rssInfo.getId(), rssInfo.getBlogName(), syncedCount, skippedCount);

        return new SyncResult(
                rssInfo.getId(),
                rssInfo.getBlogName(),
                syncedCount,
                skippedCount,
                now
        );
    }
}
