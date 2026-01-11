-- Junction table for many-to-many relationship between rss_info and tag
CREATE TABLE rss_info_tag (
    rss_info_id     BIGINT NOT NULL REFERENCES rss_info(id) ON DELETE CASCADE,
    tag_id          BIGINT NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
    PRIMARY KEY (rss_info_id, tag_id)
);

CREATE INDEX idx_rss_info_tag_rss_info ON rss_info_tag(rss_info_id);
CREATE INDEX idx_rss_info_tag_tag ON rss_info_tag(tag_id);
