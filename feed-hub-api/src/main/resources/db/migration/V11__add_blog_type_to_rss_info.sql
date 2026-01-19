-- Add blog_type column to rss_info table for categorizing blog platforms
ALTER TABLE rss_info
    ADD COLUMN blog_type VARCHAR(20) DEFAULT 'UNKNOWN';

-- Update existing records based on URL patterns
UPDATE rss_info SET blog_type = 'TISTORY' WHERE rss_url LIKE '%tistory.com%';
UPDATE rss_info SET blog_type = 'VELOG' WHERE rss_url LIKE '%velog.io%';
UPDATE rss_info SET blog_type = 'MEDIUM' WHERE rss_url LIKE '%medium.com%';
UPDATE rss_info SET blog_type = 'GITHUB_BLOG' WHERE rss_url LIKE '%github.io%';

CREATE INDEX idx_rss_info_blog_type ON rss_info(blog_type);
