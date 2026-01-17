import { useState, useEffect, useCallback } from 'react';
import { FeedList, FilterBar, AdminModal, AdminButton, TagSelectModal } from './components';
import { feedApi } from './api/client';
import type { FeedEntry } from './types';
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
  const [isAdminOpen, setIsAdminOpen] = useState(false);

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
  }, [selectedRssSources, selectedTags]);

  const handleLoadMore = async () => {
    if (loadingMore || !hasMore || !lastId) return;
    setLoadingMore(true);

    try {
      const data = await feedApi.search({
        rssSourceIds: selectedRssSources.length > 0 ? selectedRssSources : undefined,
        tagIds: selectedTags.length > 0 ? selectedTags : undefined,
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
  };

  const handleTagClick = (tagId: number) => {
    // Set only this tag as selected and trigger search
    setSelectedRssSources([]);
    setSelectedTags([tagId]);
  };

  const handleAddTag = (feedId: number) => {
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

  const handleTagUpdate = () => {
    // Refresh feeds to reflect updated tags
    fetchInitial();
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>FeedHub</h1>
        <p>RSS 피드를 한눈에</p>
      </header>

      <main className="app-main">
        <FilterBar
          selectedRssSources={selectedRssSources}
          selectedTags={selectedTags}
          onRssSourceToggle={handleRssSourceToggle}
          onTagToggle={handleTagToggle}
          onSearch={fetchInitial}
          onReset={handleReset}
        />

        <FeedList
          feeds={feeds}
          loading={loading}
          onAddTag={handleAddTag}
          onTagClick={handleTagClick}
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
    </div>
  );
}

export default App;
