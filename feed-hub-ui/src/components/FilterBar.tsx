import { useState, useEffect } from 'react';
import type { RssSource, Tag } from '../types';
import { rssSourceApi, tagApi } from '../api/client';
import './FilterBar.css';

interface FilterBarProps {
  selectedRssSources: number[];
  selectedTags: number[];
  onRssSourceToggle: (id: number) => void;
  onTagToggle: (id: number) => void;
  onSearch: () => void;
  onReset: () => void;
}

export function FilterBar({
  selectedRssSources,
  selectedTags,
  onRssSourceToggle,
  onTagToggle,
  onSearch,
  onReset,
}: FilterBarProps) {
  const [rssSources, setRssSources] = useState<RssSource[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [sourcesData, tagsData] = await Promise.all([
          rssSourceApi.getAll(),
          tagApi.getAll(),
        ]);
        setRssSources(sourcesData);
        setTags(tagsData);
      } catch (error) {
        console.error('필터 데이터 로드 실패:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) {
    return <div className="filter-bar filter-loading">필터 로딩 중...</div>;
  }

  return (
    <div className="filter-bar">
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
          {tags.length === 0 ? (
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
