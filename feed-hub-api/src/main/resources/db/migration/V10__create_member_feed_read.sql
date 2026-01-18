-- Track which feeds each member has read
CREATE TABLE member_feed_read (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    member_id BIGINT NOT NULL,
    feed_entry_id BIGINT NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_member_feed_read UNIQUE (member_id, feed_entry_id),
    CONSTRAINT fk_member_feed_read_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_feed_read_feed FOREIGN KEY (feed_entry_id) REFERENCES feed_entry(id) ON DELETE CASCADE
);

CREATE INDEX idx_member_feed_read_member ON member_feed_read(member_id);
CREATE INDEX idx_member_feed_read_feed ON member_feed_read(feed_entry_id);
