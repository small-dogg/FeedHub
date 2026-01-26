import { useState, useEffect, useCallback } from 'react';
import { FeedList, FilterBar, TagSelectModal } from '../components';
import { feedApi, subscriptionApi } from '../api/client';
import type { FeedEntry, FeedSortType, User } from '../types';

interface HomePageProps {
  user: User | null;
  onLoginRequired: () => void;
}

export function HomePage({ user, onLoginRequired }: HomePageProps) {
  const [feeds, setFeeds] = useState<FeedEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [lastId, setLastId] = useState<number | null>(null);
  const [lastPublishedAt, setLastPublishedAt] = useState<string | null>(null);
  const [lastLikeCount, setLastLikeCount] = useState<number | null>(null);
  const [lastLikedAt, setLastLikedAt] = useState<string | null>(null);
  const [selectedRssSources, setSelectedRssSources] = useState<number[]>([]);
  const [selectedTags, setSelectedTags] = useState<number[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortType, setSortType] = useState<FeedSortType>('PUBLISHED_AT');
  const [likedOnly, setLikedOnly] = useState(false);
  const [followedOnly, setFollowedOnly] = useState(false);
  const [subscriptions, setSubscriptions] = useState<number[]>([]);

  // Tag modal state
  const [isTagModalOpen, setIsTagModalOpen] = useState(false);
  const [tagModalFeedId, setTagModalFeedId] = useState<number | null>(null);
  const [tagModalCurrentTags, setTagModalCurrentTags] = useState<{ id: number; name: string }[]>([]);

  const fetchSubscriptions = useCallback(async () => {
    if (!user) {
      setSubscriptions([]);
      return;
    }
    try {
      const subs = await subscriptionApi.getMySubscriptions();
      setSubscriptions(subs);
    } catch (error) {
      console.error('구독 목록 로드 실패:', error);
    }
  }, [user]);

  useEffect(() => {
    fetchSubscriptions();
  }, [fetchSubscriptions]);

  const fetchInitial = useCallback(async () => {
    setLoading(true);
    setLastId(null);
    setLastPublishedAt(null);
    setLastLikeCount(null);
    setLastLikedAt(null);
    setHasMore(false);

    try {
      const data = await feedApi.search({
        rssSourceIds: selectedRssSources.length > 0 ? selectedRssSources : undefined,
        tagIds: selectedTags.length > 0 ? selectedTags : undefined,
        query: searchQuery.trim() || undefined,
        sortType,
        likedOnly: likedOnly || undefined,
        mode: followedOnly ? 'FOLLOWING' : undefined,
        size: 20,
      });

      setFeeds(data.content);
      setLastId(data.lastId);
      setLastPublishedAt(data.lastPublishedAt);
      setLastLikeCount(data.lastLikeCount);
      setLastLikedAt(data.lastLikedAt);
      setHasMore(data.hasMore);
    } catch (error) {
      console.error('피드 로드 실패:', error);
      setFeeds([]);
    } finally {
      setLoading(false);
    }
  }, [selectedRssSources, selectedTags, searchQuery, sortType, likedOnly, followedOnly]);

  const handleLoadMore = async () => {
    if (loadingMore || !hasMore || !lastId) return;
    setLoadingMore(true);

    try {
      const data = await feedApi.search({
        rssSourceIds: selectedRssSources.length > 0 ? selectedRssSources : undefined,
        tagIds: selectedTags.length > 0 ? selectedTags : undefined,
        query: searchQuery.trim() || undefined,
        lastId: lastId,
        lastPublishedAt: lastPublishedAt ?? undefined,
        lastLikeCount: lastLikeCount ?? undefined,
        lastLikedAt: lastLikedAt ?? undefined,
        sortType,
        likedOnly: likedOnly || undefined,
        mode: followedOnly ? 'FOLLOWING' : undefined,
        size: 20,
      });

      setFeeds(prev => [...prev, ...data.content]);
      setLastId(data.lastId);
      setLastPublishedAt(data.lastPublishedAt);
      setLastLikeCount(data.lastLikeCount);
      setLastLikedAt(data.lastLikedAt);
      setHasMore(data.hasMore);
    } catch (error) {
      console.error('피드 추가 로드 실패:', error);
    } finally {
      setLoadingMore(false);
    }
  };

  useEffect(() => {
    fetchInitial();
  }, [fetchInitial]);

  const handleRssSourceToggle = (id: number) => {
    setSelectedRssSources((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  };

  const handleTagToggle = (id: number) => {
    setSelectedTags((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  };

  const handleReset = () => {
    setSelectedRssSources([]);
    setSelectedTags([]);
    setSearchQuery('');
    setSortType('PUBLISHED_AT');
    setLikedOnly(false);
  };

  const handleTagClick = (tagId: number) => {
    setSelectedRssSources([]);
    setSelectedTags([tagId]);
  };

  const handleAddTag = (feedId: number) => {
    if (!user) {
      onLoginRequired();
      return;
    }

    const feed = feeds.find((f) => f.id === feedId);
    const currentTags = feed?.tags || [];

    setTagModalFeedId(feedId);
    setTagModalCurrentTags(currentTags);
    setIsTagModalOpen(true);
  };

  const handleTagModalClose = () => {
    setIsTagModalOpen(false);
    setTagModalFeedId(null);
    setTagModalCurrentTags([]);
  };

  const handleTagUpdate = (updatedFeed: FeedEntry) => {
    setFeeds((prev) =>
      prev.map((feed) => (feed.id === updatedFeed.id ? updatedFeed : feed))
    );
  };

  const handleMarkAsRead = (feedId: number) => {
    setFeeds((prev) =>
      prev.map((feed) =>
        feed.id === feedId
          ? { ...feed, isRead: true, viewCount: feed.viewCount + 1 }
          : feed
      )
    );
  };

  const handleLikeToggle = (feedId: number, liked: boolean, likeCount: number) => {
    setFeeds((prev) =>
      prev.map((feed) =>
        feed.id === feedId
          ? { ...feed, isLiked: liked, likeCount }
          : feed
      )
    );
  };

  const handleSubscribe = async (rssId: number) => {
    if (!user) {
      onLoginRequired();
      return;
    }
    try {
      await subscriptionApi.subscribe(rssId);
      setSubscriptions((prev) => [...prev, rssId]);
    } catch (error) {
      console.error('구독 실패:', error);
    }
  };

  const handleUnsubscribe = async (rssId: number) => {
    if (!user) {
      onLoginRequired();
      return;
    }
    try {
      await subscriptionApi.unsubscribe(rssId);
      setSubscriptions((prev) => prev.filter((id) => id !== rssId));
    } catch (error) {
      console.error('구독 취소 실패:', error);
    }
  };

  return (
    <>
      <FilterBar
        selectedRssSources={selectedRssSources}
        selectedTags={selectedTags}
        searchQuery={searchQuery}
        sortType={sortType}
        likedOnly={likedOnly}
        followedOnly={followedOnly}
        subscriptions={subscriptions}
        isLoggedIn={user !== null}
        onRssSourceToggle={handleRssSourceToggle}
        onTagToggle={handleTagToggle}
        onSearchQueryChange={setSearchQuery}
        onSortTypeChange={setSortType}
        onLikedOnlyChange={setLikedOnly}
        onFollowedOnlyChange={setFollowedOnly}
        onSubscribe={handleSubscribe}
        onUnsubscribe={handleUnsubscribe}
        onSearch={fetchInitial}
        onReset={handleReset}
      />

      <FeedList
        feeds={feeds}
        loading={loading}
        onAddTag={handleAddTag}
        onTagClick={handleTagClick}
        onMarkAsRead={handleMarkAsRead}
        onLikeToggle={handleLikeToggle}
        onLoginRequired={onLoginRequired}
        isLoggedIn={user !== null}
      />

      {hasMore && (
        <div className="load-more-container">
          <button
            className="btn-load-more"
            onClick={handleLoadMore}
            disabled={loadingMore}
          >
            {loadingMore ? '불러오는 중...' : '더보기'}
          </button>
        </div>
      )}

      <TagSelectModal
        isOpen={isTagModalOpen}
        feedId={tagModalFeedId}
        currentTags={tagModalCurrentTags}
        onClose={handleTagModalClose}
        onUpdate={handleTagUpdate}
      />
    </>
  );
}
