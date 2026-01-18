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

export function FeedCard({ feed, onAddTag, onTagClick, onMarkAsRead }: FeedCardProps) {
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);
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
  );
}
