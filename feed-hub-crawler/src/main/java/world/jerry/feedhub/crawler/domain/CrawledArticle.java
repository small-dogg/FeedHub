package world.jerry.feedhub.crawler.domain;

import java.time.Instant;

/**
 * 크롤링된 아티클 정보
 */
public record CrawledArticle(
        String title,
        String link,
        String description,
        String author,
        Instant publishedAt
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String link;
        private String description;
        private String author;
        private Instant publishedAt;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder link(String link) {
            this.link = link;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder publishedAt(Instant publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public CrawledArticle build() {
            return new CrawledArticle(title, link, description, author, publishedAt);
        }
    }
}
