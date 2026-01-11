import { useState, useEffect, useCallback } from 'react';
import { FeedList, FilterBar, Pagination, AdminModal, AdminButton } from './components';
import { feedApi } from './api/client';
import type { FeedEntry, FeedPage } from './types';
import './App.css';

function App() {
  const [feeds, setFeeds] = useState<FeedEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageInfo, setPageInfo] = useState<Omit<FeedPage, 'content'>>({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });
  const [selectedRssSources, setSelectedRssSources] = useState<number[]>([]);
  const [selectedTags, setSelectedTags] = useState<number[]>([]);
  const [isAdminOpen, setIsAdminOpen] = useState(false);

  const fetchFeeds = useCallback(async () => {
    setLoading(true);
    try {
      const data = await feedApi.search({
        rssSourceIds: selectedRssSources.length > 0 ? selectedRssSources : undefined,
        tagIds: selectedTags.length > 0 ? selectedTags : undefined,
        page,
        size: 20,
      });
      setFeeds(data.content);
      setPageInfo({
        page: data.page,
        size: data.size,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        hasNext: data.hasNext,
        hasPrevious: data.hasPrevious,
      });
    } catch (error) {
      console.error('피드 로드 실패:', error);
      setFeeds([]);
    } finally {
      setLoading(false);
    }
  }, [page, selectedRssSources, selectedTags]);

  useEffect(() => {
    fetchFeeds();
  }, [fetchFeeds]);

  const handleSearch = (rssSourceIds: number[], tagIds: number[]) => {
    setSelectedRssSources(rssSourceIds);
    setSelectedTags(tagIds);
    setPage(0);
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>FeedHub</h1>
        <p>RSS 피드를 한눈에</p>
      </header>

      <main className="app-main">
        <FilterBar onSearch={handleSearch} />

        <div className="feed-info">
          {!loading && (
            <span>총 {pageInfo.totalElements}개의 피드</span>
          )}
        </div>

        <FeedList feeds={feeds} loading={loading} />

        <Pagination
          page={pageInfo.page}
          totalPages={pageInfo.totalPages}
          hasNext={pageInfo.hasNext}
          hasPrevious={pageInfo.hasPrevious}
          onPageChange={handlePageChange}
        />
      </main>

      <AdminButton onClick={() => setIsAdminOpen(true)} />
      <AdminModal isOpen={isAdminOpen} onClose={() => setIsAdminOpen(false)} />
    </div>
  );
}

export default App;
