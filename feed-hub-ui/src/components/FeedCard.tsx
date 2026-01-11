import type { FeedEntry } from '../types';
import './FeedCard.css';

interface FeedCardProps {
  feed: FeedEntry;
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

export function FeedCard({ feed }: FeedCardProps) {
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
      <div className="feed-card-footer">
        {feed.author && <span className="feed-author">{feed.author}</span>}
        <a
          href={feed.link}
          target="_blank"
          rel="noopener noreferrer"
          className="feed-link"
        >
          원문 보기 →
        </a>
      </div>
    </div>
  );
}
