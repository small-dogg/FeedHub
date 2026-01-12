import { useState, useEffect, useCallback } from 'react';
import { FeedList, FilterBar, AdminModal, AdminButton } from './components';
import { feedApi } from './api/client';
import type { FeedEntry } from './types';
import './App.css';

function App() {
  const [feeds, setFeeds] = useState<FeedEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [lastId, setLastId] = useState<number | null>(null);
  const [selectedRssSources, setSelectedRssSources] = useState<number[]>([]);
  const [selectedTags, setSelectedTags] = useState<number[]>([]);
  const [isAdminOpen, setIsAdminOpen] = useState(false);

  const fetchInitial = useCallback(async () => {
    setLoading(true);
    setLastId(null);
    setHasMore(false);

    try {
      const data = await feedApi.search({
        rssSourceIds: selectedRssSources.length > 0 ? selectedRssSources : undefined,
        tagIds: selectedTags.length > 0 ? selectedTags : undefined,
        size: 20,
      });

      setFeeds(data.content);
      setLastId(data.lastId);
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
        size: 20,
      });

      setFeeds(prev => [...prev, ...data.content]);
      setLastId(data.lastId);
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

  const handleSearch = (rssSourceIds: number[], tagIds: number[]) => {
    setSelectedRssSources(rssSourceIds);
    setSelectedTags(tagIds);
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>FeedHub</h1>
        <p>RSS 피드를 한눈에</p>
      </header>

      <main className="app-main">
        <FilterBar onSearch={handleSearch} />

        <FeedList feeds={feeds} loading={loading} />

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
    </div>
  );
}

export default App;
