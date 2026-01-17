import type { FeedEntry } from '../types';
import './FeedCard.css';

interface FeedCardProps {
  feed: FeedEntry;
  onAddTag?: (feedId: number) => void;
  onTagClick?: (tagId: number) => void;
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

export function FeedCard({ feed, onAddTag, onTagClick }: FeedCardProps) {
  return (
    <div className="feed-card">
      <div className="feed-card-header">
        <span className="feed-blog-name">{feed.rssSource.blogName}</span>
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
        <span className="feed-author">{feed.author || ''}</span>
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
          >
            원문보기
          </a>
        </div>
      </div>
    </div>
  );
}
