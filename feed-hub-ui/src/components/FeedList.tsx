import type { FeedEntry } from '../types';
import { FeedCard } from './FeedCard';
import './FeedList.css';

interface FeedListProps {
  feeds: FeedEntry[];
  loading: boolean;
  onAddTag?: (feedId: number) => void;
  onTagClick?: (tagId: number) => void;
  onMarkAsRead?: (feedId: number) => void;
  onLikeToggle?: (feedId: number, liked: boolean, likeCount: number) => void;
  onLoginRequired?: () => void;
  isLoggedIn?: boolean;
}

export function FeedList({ feeds, loading, onAddTag, onTagClick, onMarkAsRead, onLikeToggle, onLoginRequired, isLoggedIn }: FeedListProps) {
  if (loading) {
    return (
      <div className="feed-list-loading">
        <div className="spinner"></div>
        <p>피드를 불러오는 중...</p>
      </div>
    );
  }

  if (feeds.length === 0) {
    return (
      <div className="feed-list-empty">
        <p>표시할 피드가 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="feed-list">
      {feeds.map((feed) => (
        <FeedCard
          key={feed.id}
          feed={feed}
          onAddTag={onAddTag}
          onTagClick={onTagClick}
          onMarkAsRead={onMarkAsRead}
          onLikeToggle={onLikeToggle}
          onLoginRequired={onLoginRequired}
          isLoggedIn={isLoggedIn}
        />
      ))}
    </div>
  );
}
