CREATE TABLE subscription (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    rss_info_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_subscription_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    CONSTRAINT fk_subscription_rss_info FOREIGN KEY (rss_info_id) REFERENCES rss_info(id) ON DELETE CASCADE,
    CONSTRAINT uk_subscription_member_rss UNIQUE (member_id, rss_info_id)
);

CREATE INDEX idx_subscription_member_id ON subscription(member_id);
