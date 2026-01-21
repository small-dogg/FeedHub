import { useState, useEffect } from 'react';
import type { RssSource, Tag, FeedSortType } from '../types';
import { rssSourceApi, tagApi } from '../api/client';
import './FilterBar.css';

interface FilterBarProps {
  selectedRssSources: number[];
  selectedTags: number[];
  searchQuery: string;
  sortType: FeedSortType;
  likedOnly: boolean;
  isLoggedIn: boolean;
  onRssSourceToggle: (id: number) => void;
  onTagToggle: (id: number) => void;
  onSearchQueryChange: (query: string) => void;
  onSortTypeChange: (sortType: FeedSortType) => void;
  onLikedOnlyChange: (likedOnly: boolean) => void;
  onSearch: () => void;
  onReset: () => void;
}

export function FilterBar({
  selectedRssSources,
  selectedTags,
  searchQuery,
  sortType,
  likedOnly,
  isLoggedIn,
  onRssSourceToggle,
  onTagToggle,
  onSearchQueryChange,
  onSortTypeChange,
  onLikedOnlyChange,
  onSearch,
  onReset,
}: FilterBarProps) {
  const [rssSources, setRssSources] = useState<RssSource[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const sourcesData = await rssSourceApi.getAll();
        setRssSources(sourcesData);

        if (isLoggedIn) {
          const tagsData = await tagApi.getAll();
          setTags(tagsData);
        } else {
          setTags([]);
        }
      } catch (error) {
        console.error('필터 데이터 로드 실패:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [isLoggedIn]);

  if (loading) {
    return <div className="filter-bar filter-loading">필터 로딩 중...</div>;
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      onSearch();
    }
  };

  const handleSortChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value as FeedSortType;
    onSortTypeChange(value);
    // LIKED_AT 선택 시 likedOnly도 자동 활성화
    if (value === 'LIKED_AT') {
      onLikedOnlyChange(true);
    }
  };

  const handleLikedOnlyChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const checked = e.target.checked;
    onLikedOnlyChange(checked);
    // likedOnly 해제 시 LIKED_AT 정렬은 PUBLISHED_AT으로 변경
    if (!checked && sortType === 'LIKED_AT') {
      onSortTypeChange('PUBLISHED_AT');
    }
  };

  return (
    <div className="filter-bar">
      <div className="filter-section search-section">
        <h4>검색</h4>
        <input
          type="text"
          className="search-input"
          placeholder="제목, 내용 검색..."
          value={searchQuery}
          onChange={(e) => onSearchQueryChange(e.target.value)}
          onKeyDown={handleKeyDown}
        />
      </div>

      <div className="filter-section">
        <h4>정렬</h4>
        <div className="sort-options">
          <select
            className="sort-select"
            value={sortType}
            onChange={handleSortChange}
          >
            <option value="PUBLISHED_AT">최신순</option>
            <option value="LIKE_COUNT">좋아요순</option>
            {isLoggedIn && likedOnly && (
              <option value="LIKED_AT">좋아요한 시간순</option>
            )}
          </select>
          {isLoggedIn && (
            <label className="liked-only-label">
              <input
                type="checkbox"
                checked={likedOnly}
                onChange={handleLikedOnlyChange}
              />
              내가 좋아요한 피드만
            </label>
          )}
        </div>
      </div>

      <div className="filter-section">
        <h4>RSS 소스</h4>
        <div className="filter-chips">
          {rssSources.length === 0 ? (
            <span className="filter-empty">등록된 소스가 없습니다</span>
          ) : (
            rssSources.map((source) => (
              <button
                key={source.id}
                className={`filter-chip ${selectedRssSources.includes(source.id) ? 'active' : ''}`}
                onClick={() => onRssSourceToggle(source.id)}
              >
                {source.blogName}
              </button>
            ))
          )}
        </div>
      </div>

      <div className="filter-section">
        <h4>태그</h4>
        <div className="filter-chips">
          {!isLoggedIn ? (
            <span className="filter-empty">로그인 후 태그를 사용할 수 있습니다</span>
          ) : tags.length === 0 ? (
            <span className="filter-empty">등록된 태그가 없습니다</span>
          ) : (
            tags.map((tag) => (
              <button
                key={tag.id}
                className={`filter-chip ${selectedTags.includes(tag.id) ? 'active' : ''}`}
                onClick={() => onTagToggle(tag.id)}
              >
                #{tag.name}
              </button>
            ))
          )}
        </div>
      </div>

      <div className="filter-actions">
        <button className="btn btn-secondary" onClick={onReset}>
          초기화
        </button>
        <button className="btn btn-primary" onClick={onSearch}>
          검색
        </button>
      </div>
    </div>
  );
}
