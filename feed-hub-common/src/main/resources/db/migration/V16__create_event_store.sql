-- Event Store for event sourcing
CREATE TABLE event_store (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL UNIQUE,
    correlation_id  VARCHAR(100),
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    event_data      JSONB NOT NULL,
    metadata        JSONB,
    version         INTEGER NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for querying
CREATE INDEX idx_event_store_aggregate ON event_store(aggregate_type, aggregate_id);
CREATE INDEX idx_event_store_event_type ON event_store(event_type);
CREATE INDEX idx_event_store_correlation ON event_store(correlation_id);
CREATE INDEX idx_event_store_created_at ON event_store(created_at DESC);

-- Unique constraint for idempotency
CREATE UNIQUE INDEX idx_event_store_aggregate_version
    ON event_store(aggregate_type, aggregate_id, version);

-- Sync history table for simplified tracking
CREATE TABLE sync_history (
    id              BIGSERIAL PRIMARY KEY,
    command_id      VARCHAR(100) NOT NULL,
    rss_info_id     BIGINT REFERENCES rss_info(id) ON DELETE CASCADE,
    sync_type       VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    new_entries     INTEGER NOT NULL DEFAULT 0,
    skipped_entries INTEGER NOT NULL DEFAULT 0,
    error_message   TEXT,
    started_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at    TIMESTAMP WITH TIME ZONE,
    requested_by    VARCHAR(100)
);

CREATE INDEX idx_sync_history_rss_info ON sync_history(rss_info_id);
CREATE INDEX idx_sync_history_command_id ON sync_history(command_id);
CREATE INDEX idx_sync_history_started_at ON sync_history(started_at DESC);
