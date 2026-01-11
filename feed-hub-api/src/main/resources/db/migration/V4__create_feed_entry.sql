-- Feed entries (articles) from RSS sources
CREATE TABLE feed_entry (
    id              BIGSERIAL PRIMARY KEY,
    rss_info_id     BIGINT NOT NULL REFERENCES rss_info(id) ON DELETE CASCADE,
    title           VARCHAR(500) NOT NULL,
    link            VARCHAR(2048) NOT NULL,
    description     TEXT,
    author          VARCHAR(255),
    published_at    TIMESTAMP WITH TIME ZONE,
    guid            VARCHAR(2048),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_feed_entry_rss_guid UNIQUE (rss_info_id, guid)
);

CREATE INDEX idx_feed_entry_rss_info ON feed_entry(rss_info_id);
CREATE INDEX idx_feed_entry_published_at ON feed_entry(published_at DESC);
CREATE INDEX idx_feed_entry_created_at ON feed_entry(created_at DESC);
