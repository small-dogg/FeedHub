package world.jerry.feedhub.collector.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.collector.domain.FeedEntry;
import world.jerry.feedhub.collector.infrastructure.rss.ParsedFeedEntry;
import world.jerry.feedhub.collector.infrastructure.rss.RssParser;
import world.jerry.feedhub.collector.repository.FeedEntryRepository;
import world.jerry.feedhub.common.command.SyncRssFeedCommand;
import world.jerry.feedhub.common.event.FeedEntriesCollectedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RSS 피드 동기화 처리 핸들러
 * SyncRssFeedCommand를 받아 RSS를 파싱하고 FeedEntry를 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RssSyncHandler {

    private final FeedEntryRepository feedEntryRepository;
    private final RssParser rssParser;

    /**
     * RSS 동기화 Command 처리
     */
    @Transactional
    public FeedEntriesCollectedEvent handle(SyncRssFeedCommand command) {
        log.info("RSS 동기화 시작: rssInfoId={}, blogName={}, url={}",
                command.rssInfoId(), command.blogName(), command.rssUrl());

        List<ParsedFeedEntry> entries = rssParser.parse(command.rssUrl());

        int newEntriesCount = 0;
        int skippedCount = 0;
        List<Long> savedEntryIds = new ArrayList<>();

        for (ParsedFeedEntry entry : entries) {
            if (entry.guid() == null || entry.guid().isBlank()) {
                log.warn("GUID가 없는 엔트리 건너뜀: title={}", entry.title());
                skippedCount++;
                continue;
            }

            boolean exists = feedEntryRepository.existsByGuid(entry.guid());
            if (exists) {
                skippedCount++;
                continue;
            }

            FeedEntry feedEntry = FeedEntry.builder()
                    .rssInfoId(command.rssInfoId())
                    .title(entry.title())
                    .link(entry.link())
                    .description(entry.description())
                    .author(entry.author())
                    .publishedAt(entry.publishedAt())
                    .guid(entry.guid())
                    .build();

            FeedEntry saved = feedEntryRepository.save(feedEntry);
            savedEntryIds.add(saved.getId());
            newEntriesCount++;
        }

        log.info("RSS 동기화 완료: rssInfoId={}, blogName={}, new={}, skipped={}",
                command.rssInfoId(), command.blogName(), newEntriesCount, skippedCount);

        return new FeedEntriesCollectedEvent(
                UUID.randomUUID().toString(),
                command.commandId(),
                command.rssInfoId(),
                command.blogName(),
                newEntriesCount,
                skippedCount,
                savedEntryIds,
                Instant.now()
        );
    }
}
