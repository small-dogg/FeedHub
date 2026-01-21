-- Add CHECK constraint for sync_type
ALTER TABLE sync_history
ADD CONSTRAINT chk_sync_type CHECK (sync_type IN ('RSS_SYNC', 'CRAWL'));
