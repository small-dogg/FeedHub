-- Feed like table for user's feed likes
CREATE TABLE feed_like (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    member_id BIGINT NOT NULL,
    feed_entry_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_feed_like_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    CONSTRAINT fk_feed_like_feed_entry FOREIGN KEY (feed_entry_id) REFERENCES feed_entry(id) ON DELETE CASCADE,
    CONSTRAINT uq_feed_like_member_feed UNIQUE (member_id, feed_entry_id)
);

-- Index for counting likes per feed
CREATE INDEX idx_feed_like_feed_entry_id ON feed_like(feed_entry_id);

-- Index for finding user's liked feeds
CREATE INDEX idx_feed_like_member_id ON feed_like(member_id);
