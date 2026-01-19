package world.jerry.feedhub.crawler.message;

/**
 * 크롤링 모드
 */
public enum CrawlMode {
    FULL,         // 전체 페이지 크롤링 (최초 등록 시)
    INCREMENTAL   // 최근 페이지만 크롤링 (주기적 동기화)
}
