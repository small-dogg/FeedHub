-- Add view_count column to feed_entry table
ALTER TABLE feed_entry ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;

-- Index for sorting by view_count (optional, for popular feeds feature)
CREATE INDEX idx_feed_entry_view_count ON feed_entry(view_count DESC);
