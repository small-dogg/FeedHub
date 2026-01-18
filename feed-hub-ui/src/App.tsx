import { useState, useEffect, useCallback } from 'react';
import { FeedList, FilterBar, AdminModal, AdminButton, TagSelectModal, AuthModal } from './components';
import { feedApi, authApi, tokenManager } from './api/client';
import type { FeedEntry, User } from './types';
import './App.css';

function App() {
  const [feeds, setFeeds] = useState<FeedEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [lastId, setLastId] = useState<number | null>(null);
  const [lastPublishedAt, setLastPublishedAt] = useState<string | null>(null);
  const [selectedRssSources, setSelectedRssSources] = useState<number[]>([]);
  const [selectedTags, setSelectedTags] = useState<number[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isAdminOpen, setIsAdminOpen] = useState(false);

  // Auth state
  const [user, setUser] = useState<User | null>(null);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);

  // Tag modal state
  const [isTagModalOpen, setIsTagModalOpen] = useState(false);
  const [tagModalFeedId, setTagModalFeedId] = useState<number | null>(null);
  const [tagModalCurrentTags, setTagModalCurrentTags] = useState<{ id: number; name: string }[]>([]);

  const fetchInitial = useCallback(async () => {
    setLoading(true);
    setLastId(null);
    setLastPublishedAt(null);
    setHasMore(false);

    try {
      const data = await feedApi.search({
        rssSourceIds: selectedRssSources.length > 0 ? selectedRssSources : undefined,
        tagIds: selectedTags.length > 0 ? selectedTags : undefined,
        query: searchQuery.trim() || undefined,
        size: 20,
      });

      setFeeds(data.content);
      setLastId(data.lastId);
      setLastPublishedAt(data.lastPublishedAt);
      setHasMore(data.hasMore);
    } catch (error) {
      console.error('피드 로드 실패:', error);
      setFeeds([]);
    } finally {
      setLoading(false);
    }
  }, [selectedRssSources, selectedTags, searchQuery]);

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
        size: 20,
      });

      setFeeds(prev => [...prev, ...data.content]);
      setLastId(data.lastId);
      setLastPublishedAt(data.lastPublishedAt);
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

  // Check for existing auth token on mount
  useEffect(() => {
    const token = tokenManager.getToken();
    if (token) {
      authApi.getMe()
        .then((userData) => setUser(userData))
        .catch(() => {
          // Token is invalid, remove it
          tokenManager.removeToken();
        });
    }
  }, []);

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
  };

  const handleTagClick = (tagId: number) => {
    // Set only this tag as selected and trigger search
    setSelectedRssSources([]);
    setSelectedTags([tagId]);
  };

  const handleAddTag = (feedId: number) => {
    // Check if user is logged in
    if (!user) {
      setIsAuthModalOpen(true);
      return;
    }

    // Find current tags from the feed
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
    // Update only the modified feed without full refresh
    setFeeds((prev) =>
      prev.map((feed) => (feed.id === updatedFeed.id ? updatedFeed : feed))
    );
  };

  const handleViewCountUpdate = (feedId: number) => {
    // Increment viewCount locally for immediate feedback
    setFeeds((prev) =>
      prev.map((feed) =>
        feed.id === feedId ? { ...feed, viewCount: feed.viewCount + 1 } : feed
      )
    );
  };

  const handleAuthSuccess = (userData: User, token: string) => {
    tokenManager.setToken(token);
    setUser(userData);
    setIsAuthModalOpen(false);
    // 로그인 후 피드 목록 새로고침 (태그 정보 갱신)
    fetchInitial();
  };

  const handleLogout = () => {
    tokenManager.removeToken();
    setUser(null);
    // 로그아웃 후 피드 목록 새로고침 (태그 정보 제거)
    fetchInitial();
  };

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-left">
          <h1>FeedHub</h1>
          <p>RSS 피드를 한눈에</p>
        </div>
        <div className="header-right">
          {user ? (
            <div className="user-info">
              <span className="user-nickname">{user.nickname}</span>
              <button type="button" className="btn-logout" onClick={handleLogout}>
                로그아웃
              </button>
            </div>
          ) : (
            <button
              type="button"
              className="btn-login"
              onClick={() => setIsAuthModalOpen(true)}
            >
              로그인
            </button>
          )}
        </div>
      </header>

      <main className="app-main">
        <FilterBar
          selectedRssSources={selectedRssSources}
          selectedTags={selectedTags}
          searchQuery={searchQuery}
          isLoggedIn={user !== null}
          onRssSourceToggle={handleRssSourceToggle}
          onTagToggle={handleTagToggle}
          onSearchQueryChange={setSearchQuery}
          onSearch={fetchInitial}
          onReset={handleReset}
        />

        <FeedList
          feeds={feeds}
          loading={loading}
          onAddTag={handleAddTag}
          onTagClick={handleTagClick}
          onViewCountUpdate={handleViewCountUpdate}
        />

        {/* Load more button */}
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
      </main>

      <AdminButton onClick={() => setIsAdminOpen(true)} />
      <AdminModal isOpen={isAdminOpen} onClose={() => setIsAdminOpen(false)} />
      <TagSelectModal
        isOpen={isTagModalOpen}
        feedId={tagModalFeedId}
        currentTags={tagModalCurrentTags}
        onClose={handleTagModalClose}
        onUpdate={handleTagUpdate}
      />
      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        onAuthSuccess={handleAuthSuccess}
      />
    </div>
  );
}

export default App;
