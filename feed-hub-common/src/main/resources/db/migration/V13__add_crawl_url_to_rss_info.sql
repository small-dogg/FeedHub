-- Add crawl_url column to rss_info for explicit crawl target URL
ALTER TABLE rss_info
    ADD COLUMN crawl_url VARCHAR(2048);

-- For existing MEDIUM type entries, set crawl_url from site_url if available
UPDATE rss_info
SET crawl_url = site_url
WHERE site_url IS NOT NULL AND blog_type = 'MEDIUM';
