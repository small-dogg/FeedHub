package world.jerry.feedhub.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.collector.domain.CrawledArticle;
import world.jerry.feedhub.collector.domain.FeedEntry;
import world.jerry.feedhub.collector.repository.FeedEntryRepository;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
                String normalizedLink = normalizeUrl(article.link());

                // 중복 체크 (원본 링크와 정규화된 링크 모두 확인)
                if (feedEntryRepository.existsByLink(article.link()) ||
                    feedEntryRepository.existsByLink(normalizedLink)) {
                    log.debug("Skipping duplicate article: {}", article.link());
                    continue;
                }

                // 정규화된 링크로 저장
                CrawledArticle normalizedArticle = CrawledArticle.builder()
                        .title(article.title())
                        .link(normalizedLink)
                        .description(article.description())
                        .author(article.author())
                        .publishedAt(article.publishedAt())
                        .build();

                FeedEntry feedEntry = FeedEntry.from(normalizedArticle, rssInfoId);
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

    /**
     * URL 정규화 (디코딩하여 일관된 형식 유지)
     */
    private String normalizeUrl(String url) {
        if (url == null) return null;
        try {
            return URLDecoder.decode(url, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return url;
        }
    }
}
