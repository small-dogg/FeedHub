-- Remove rss_info_tag relationship (tags were on RSS source level)
DROP TABLE IF EXISTS rss_info_tag;

-- Create feed_entry_tag junction table (tags on feed entry level)
CREATE TABLE feed_entry_tag (
    feed_entry_id   BIGINT NOT NULL REFERENCES feed_entry(id) ON DELETE CASCADE,
    tag_id          BIGINT NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
    PRIMARY KEY (feed_entry_id, tag_id)
);

CREATE INDEX idx_feed_entry_tag_feed_entry ON feed_entry_tag(feed_entry_id);
CREATE INDEX idx_feed_entry_tag_tag ON feed_entry_tag(tag_id);
