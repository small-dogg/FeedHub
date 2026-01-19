package world.jerry.feedhub.crawler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.crawler.domain.CrawledArticle;
import world.jerry.feedhub.crawler.domain.FeedEntry;
import world.jerry.feedhub.crawler.repository.FeedEntryRepository;

import java.util.List;

/**
 * 크롤링된 아티클을 피드 엔트리로 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedEntrySaveService {

    private final FeedEntryRepository feedEntryRepository;

    /**
     * 크롤링된 아티클 목록을 저장
     * 중복 링크는 건너뜀
     */
    @Transactional
    public int saveAll(List<CrawledArticle> articles, Long rssInfoId) {
        int savedCount = 0;

        for (CrawledArticle article : articles) {
            try {
                // 중복 체크 (link 기준)
                if (feedEntryRepository.existsByLink(article.link())) {
                    log.debug("Skipping duplicate article: {}", article.link());
                    continue;
                }

                FeedEntry feedEntry = FeedEntry.from(article, rssInfoId);
                feedEntryRepository.save(feedEntry);
                savedCount++;

            } catch (Exception e) {
                log.warn("Failed to save article: link={}, error={}",
                        article.link(), e.getMessage());
            }
        }

        log.info("Saved {} articles for rssInfoId={}", savedCount, rssInfoId);
        return savedCount;
    }
}
