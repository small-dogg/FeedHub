-- RSS source information table
CREATE TABLE rss_info (
    id              BIGSERIAL PRIMARY KEY,
    blog_name       VARCHAR(255) NOT NULL,
    author          VARCHAR(255),
    rss_url         VARCHAR(2048) NOT NULL UNIQUE,
    site_url        VARCHAR(2048),
    language        VARCHAR(10),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_sync_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_rss_info_blog_name ON rss_info(blog_name);
CREATE INDEX idx_rss_info_language ON rss_info(language);
CREATE INDEX idx_rss_info_last_sync_at ON rss_info(last_sync_at);
