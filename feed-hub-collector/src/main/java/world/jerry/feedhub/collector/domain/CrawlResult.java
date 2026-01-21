package world.jerry.feedhub.collector.domain;

import java.util.List;

/**
 * 크롤링 결과
 */
public record CrawlResult(
        Long rssInfoId,
        List<CrawledArticle> articles,
        int totalPages,
        boolean success,
        String errorMessage
) {
    public static CrawlResult success(Long rssInfoId, List<CrawledArticle> articles, int totalPages) {
        return new CrawlResult(rssInfoId, articles, totalPages, true, null);
    }

    public static CrawlResult failure(Long rssInfoId, String errorMessage) {
        return new CrawlResult(rssInfoId, List.of(), 0, false, errorMessage);
    }

    public int getArticleCount() {
        return articles != null ? articles.size() : 0;
    }
}
