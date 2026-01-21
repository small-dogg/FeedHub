-- Enable pg_trgm extension for trigram-based text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- GIN indexes for text search on title and description
CREATE INDEX idx_feed_entry_title_trgm
    ON feed_entry USING GIN (title gin_trgm_ops);

CREATE INDEX idx_feed_entry_description_trgm
    ON feed_entry USING GIN (description gin_trgm_ops);
