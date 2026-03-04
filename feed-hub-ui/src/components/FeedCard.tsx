import { useState, useRef } from 'react';
import type { FeedEntry } from '../types';
import { feedApi } from '../api/client';
import { ContentPreviewModal } from './ContentPreviewModal';
import './FeedCard.css';

interface FeedCardProps {
  feed: FeedEntry;
  onAddTag?: (feedId: number) => void;
  onTagClick?: (tagId: number) => void;
  onMarkAsRead?: (feedId: number) => void;
  onLikeToggle?: (feedId: number, liked: boolean, likeCount: number) => void;
  onLoginRequired?: () => void;
  isLoggedIn?: boolean;
  isSelected?: boolean;
  onSelect?: (feedId: number) => void;
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

export function FeedCard({ feed, onAddTag, onTagClick, onMarkAsRead, onLikeToggle, onLoginRequired, isLoggedIn, isSelected, onSelect }: FeedCardProps) {
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);
  const [isLiking, setIsLiking] = useState(false);
  const [copyStatus, setCopyStatus] = useState<'idle' | 'copied'>('idle');
  // 이번 세션에서 API를 호출했는지 추적 (중복 호출 방지)
  const hasCalledApiRef = useRef(false);

  // 읽음 처리 및 조회수 증가 (한 번만 호출)
  const markAsReadOnce = () => {
    if (feed.isRead || hasCalledApiRef.current) {
      return; // 이미 읽음 상태이거나 이미 API 호출함
    }
    hasCalledApiRef.current = true;
    feedApi.incrementViewCount(feed.id).then(() => {
      onMarkAsRead?.(feed.id);
    }).catch(() => {
      // 실패 시 다시 시도할 수 있도록 리셋
      hasCalledApiRef.current = false;
    });
  };

  const handlePreviewOpen = () => {
    setIsPreviewOpen(true);
    markAsReadOnce();
  };

  const handleViewClick = () => {
    markAsReadOnce();
  };

  const handleCopyUrl = async () => {
    try {
      await navigator.clipboard.writeText(feed.link);
      setCopyStatus('copied');
      setTimeout(() => setCopyStatus('idle'), 1500);
    } catch {
      // Fallback for older browsers
      const textarea = document.createElement('textarea');
      textarea.value = feed.link;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      setCopyStatus('copied');
      setTimeout(() => setCopyStatus('idle'), 1500);
    }
  };

  const handleLikeClick = async () => {
    if (!isLoggedIn) {
      onLoginRequired?.();
      return;
    }
    if (isLiking) return;

    setIsLiking(true);
    try {
      const result = await feedApi.toggleLike(feed.id);
      onLikeToggle?.(feed.id, result.liked, result.likeCount);
    } catch (error) {
      console.error('좋아요 실패:', error);
    } finally {
      setIsLiking(false);
    }
  };

  return (
    <div className={`feed-card-wrapper${isSelected ? ' selected' : ''}`}>
      <div className="feed-checkbox-col">
        <input
          type="checkbox"
          className="feed-checkbox"
          checked={isSelected ?? false}
          onChange={() => onSelect?.(feed.id)}
          onClick={(e) => e.stopPropagation()}
          aria-label="Select feed"
        />
      </div>
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
        <div className="feed-header-right">
          <span className={`feed-read-status ${feed.isRead ? 'read' : 'unread'}`}>
            {feed.isRead ? '읽음' : '읽지않음'}
          </span>
          <span className="feed-date">{formatDate(feed.publishedAt)}</span>
        </div>
      </div>
      <h3 className="feed-title">
        <a href={feed.link} target="_blank" rel="noopener noreferrer" onClick={handleViewClick}>
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
          <button
            type="button"
            className={`btn-copy-url ${copyStatus === 'copied' ? 'copied' : ''}`}
            onClick={handleCopyUrl}
          >
            {copyStatus === 'copied' ? '복사됨!' : 'URL 복사'}
          </button>
          <button
            type="button"
            className="btn-preview"
            onClick={handlePreviewOpen}
          >
            미리보기
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

        <ContentPreviewModal
          isOpen={isPreviewOpen}
          title={feed.title}
          content={feed.description}
          link={feed.link}
          onClose={() => setIsPreviewOpen(false)}
        />
      </div>
      <div className="feed-like-section">
        <button
          type="button"
          className={`btn-like ${feed.isLiked ? 'liked' : ''}`}
          onClick={handleLikeClick}
          disabled={isLiking}
          aria-label={feed.isLiked ? '좋아요 취소' : '좋아요'}
        >
          <svg
            className="heart-icon"
            viewBox="0 0 24 24"
            fill={feed.isLiked ? 'currentColor' : 'none'}
            stroke="currentColor"
            strokeWidth="2"
          >
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
          </svg>
        </button>
        {feed.likeCount > 0 && (
          <span className="like-count">{feed.likeCount}</span>
        )}
      </div>
    </div>
  );
}
