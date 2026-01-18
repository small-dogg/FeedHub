import type { FeedEntry } from '../types';
import { feedApi } from '../api/client';
import './FeedCard.css';

interface FeedCardProps {
  feed: FeedEntry;
  onAddTag?: (feedId: number) => void;
  onTagClick?: (tagId: number) => void;
  onViewCountUpdate?: (feedId: number) => void;
}

function formatDate(dateString: string | null): string {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function FeedCard({ feed, onAddTag, onTagClick, onViewCountUpdate }: FeedCardProps) {
  const handleViewClick = () => {
    // 조회수 증가 API 호출 (비동기, 에러 무시)
    feedApi.incrementViewCount(feed.id).then(() => {
      onViewCountUpdate?.(feed.id);
    }).catch(() => {
      // 조회수 증가 실패는 무시
    });
  };
  return (
    <div className="feed-card">
      <div className="feed-card-header">
        {feed.rssSource.siteUrl ? (
          <a
            href={feed.rssSource.siteUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="feed-blog-name"
          >
            {feed.rssSource.blogName}
          </a>
        ) : (
          <span className="feed-blog-name">{feed.rssSource.blogName}</span>
        )}
        <span className="feed-date">{formatDate(feed.publishedAt)}</span>
      </div>
      <h3 className="feed-title">
        <a href={feed.link} target="_blank" rel="noopener noreferrer">
          {feed.title}
        </a>
      </h3>
      {feed.tags && feed.tags.length > 0 && (
        <div className="feed-tags">
          {feed.tags.map((tag) => (
            <button
              key={tag.id}
              type="button"
              className="feed-tag"
              onClick={() => onTagClick?.(tag.id)}
            >
              #{tag.name}
            </button>
          ))}
        </div>
      )}
      <div className="feed-card-footer">
        <div className="feed-footer-left">
          <span className="feed-author">{feed.author || ''}</span>
          {feed.viewCount > 0 && (
            <span className="feed-view-count">조회 {feed.viewCount}</span>
          )}
        </div>
        <div className="feed-card-actions">
          <button
            type="button"
            className="btn-add-tag"
            onClick={() => onAddTag?.(feed.id)}
          >
            태그추가
          </button>
          <a
            href={feed.link}
            target="_blank"
            rel="noopener noreferrer"
            className="feed-link"
            onClick={handleViewClick}
          >
            원문보기
          </a>
        </div>
      </div>
    </div>
  );
}
