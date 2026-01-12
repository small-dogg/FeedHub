import { useState, useEffect } from 'react';
import type { RssSource, Tag } from '../types';
import { rssSourceApi, tagApi } from '../api/client';
import './AdminModal.css';

interface AdminModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function AdminModal({ isOpen, onClose }: AdminModalProps) {
  const [activeTab, setActiveTab] = useState<'sources' | 'tags'>('sources');
  const [rssSources, setRssSources] = useState<RssSource[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(false);
  const [syncingId, setSyncingId] = useState<number | null>(null);
  const [syncingAll, setSyncingAll] = useState(false);

  // Form states
  const [newSource, setNewSource] = useState({
    blogName: '',
    author: '',
    rssUrl: '',
    siteUrl: '',
    language: 'ko',
  });
  const [newTagName, setNewTagName] = useState('');

  useEffect(() => {
    if (isOpen) {
      fetchData();
    }
  }, [isOpen]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [sourcesData, tagsData] = await Promise.all([
        rssSourceApi.getAll(),
        tagApi.getAll(),
      ]);
      setRssSources(sourcesData);
      setTags(tagsData);
    } catch (error) {
      console.error('데이터 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddSource = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSource.blogName || !newSource.rssUrl) {
      alert('블로그 이름과 RSS URL은 필수입니다.');
      return;
    }
    try {
      await rssSourceApi.create({
        blogName: newSource.blogName,
        author: newSource.author || undefined,
        rssUrl: newSource.rssUrl,
        siteUrl: newSource.siteUrl || undefined,
        language: newSource.language || undefined,
      });
      setNewSource({ blogName: '', author: '', rssUrl: '', siteUrl: '', language: 'ko' });
      fetchData();
    } catch (error) {
      console.error('RSS 소스 추가 실패:', error);
      alert('RSS 소스 추가에 실패했습니다.');
    }
  };

  const handleDeleteSource = async (id: number) => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    try {
      await rssSourceApi.delete(id);
      fetchData();
    } catch (error) {
      console.error('RSS 소스 삭제 실패:', error);
      alert('RSS 소스 삭제에 실패했습니다.');
    }
  };

  const handleAddTag = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTagName.trim()) {
      alert('태그 이름을 입력하세요.');
      return;
    }
    try {
      await tagApi.create(newTagName.trim());
      setNewTagName('');
      fetchData();
    } catch (error) {
      console.error('태그 추가 실패:', error);
      alert('태그 추가에 실패했습니다.');
    }
  };

  const handleDeleteTag = async (id: number) => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    try {
      await tagApi.delete(id);
      fetchData();
    } catch (error) {
      console.error('태그 삭제 실패:', error);
      alert('태그 삭제에 실패했습니다.');
    }
  };

  const handleSync = async (id: number) => {
    setSyncingId(id);
    try {
      const result = await rssSourceApi.sync(id);
      alert(`동기화 완료: ${result.syncedCount}개 추가, ${result.skippedCount}개 건너뜀`);
      fetchData();
    } catch (error) {
      console.error('동기화 실패:', error);
      alert('동기화에 실패했습니다.');
    } finally {
      setSyncingId(null);
    }
  };

  const handleSyncAll = async () => {
    if (!confirm('모든 RSS 소스를 동기화하시겠습니까?')) return;
    setSyncingAll(true);
    try {
      const results = await rssSourceApi.syncAll();
      const totalSynced = results.reduce((sum, r) => sum + r.syncedCount, 0);
      const totalSkipped = results.reduce((sum, r) => sum + r.skippedCount, 0);
      alert(`전체 동기화 완료: ${totalSynced}개 추가, ${totalSkipped}개 건너뜀`);
      fetchData();
    } catch (error) {
      console.error('전체 동기화 실패:', error);
      alert('전체 동기화에 실패했습니다.');
    } finally {
      setSyncingAll(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>관리자 설정</h2>
          <button className="modal-close" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="modal-tabs">
          <button
            className={`modal-tab ${activeTab === 'sources' ? 'active' : ''}`}
            onClick={() => setActiveTab('sources')}
          >
            RSS 소스
          </button>
          <button
            className={`modal-tab ${activeTab === 'tags' ? 'active' : ''}`}
            onClick={() => setActiveTab('tags')}
          >
            태그
          </button>
        </div>

        <div className="modal-body">
          {loading ? (
            <div className="modal-loading">로딩 중...</div>
          ) : activeTab === 'sources' ? (
            <div className="admin-section">
              <form className="admin-form" onSubmit={handleAddSource}>
                <h4>새 RSS 소스 추가</h4>
                <div className="form-row">
                  <input
                    type="text"
                    placeholder="블로그 이름 *"
                    value={newSource.blogName}
                    onChange={(e) =>
                      setNewSource({ ...newSource, blogName: e.target.value })
                    }
                  />
                  <input
                    type="text"
                    placeholder="작성자"
                    value={newSource.author}
                    onChange={(e) =>
                      setNewSource({ ...newSource, author: e.target.value })
                    }
                  />
                </div>
                <div className="form-row">
                  <input
                    type="url"
                    placeholder="RSS URL *"
                    value={newSource.rssUrl}
                    onChange={(e) =>
                      setNewSource({ ...newSource, rssUrl: e.target.value })
                    }
                  />
                </div>
                <div className="form-row">
                  <input
                    type="url"
                    placeholder="사이트 URL"
                    value={newSource.siteUrl}
                    onChange={(e) =>
                      setNewSource({ ...newSource, siteUrl: e.target.value })
                    }
                  />
                  <select
                    value={newSource.language}
                    onChange={(e) =>
                      setNewSource({ ...newSource, language: e.target.value })
                    }
                  >
                    <option value="ko">한국어</option>
                    <option value="en">영어</option>
                    <option value="ja">일본어</option>
                  </select>
                </div>
                <button type="submit" className="btn btn-primary">
                  추가
                </button>
              </form>

              <div className="admin-list">
                <div className="list-header">
                  <h4>등록된 RSS 소스 ({rssSources.length})</h4>
                  {rssSources.length > 0 && (
                    <button
                      className="btn btn-sync-all"
                      onClick={handleSyncAll}
                      disabled={syncingAll || syncingId !== null}
                    >
                      {syncingAll ? '동기화 중...' : '전체 동기화'}
                    </button>
                  )}
                </div>
                {rssSources.length === 0 ? (
                  <p className="empty-message">등록된 RSS 소스가 없습니다.</p>
                ) : (
                  <ul>
                    {rssSources.map((source) => (
                      <li key={source.id}>
                        <div className="list-item-info">
                          <strong>{source.blogName}</strong>
                          <span className="list-item-url">{source.rssUrl}</span>
                          {source.lastSyncAt && (
                            <span className="list-item-sync-time">
                              마지막 동기화: {new Date(source.lastSyncAt).toLocaleString('ko-KR')}
                            </span>
                          )}
                        </div>
                        <div className="list-item-actions">
                          <button
                            className="btn-sync"
                            onClick={() => handleSync(source.id)}
                            disabled={syncingId !== null || syncingAll}
                          >
                            {syncingId === source.id ? '동기화 중...' : '동기화'}
                          </button>
                          <button
                            className="btn-delete"
                            onClick={() => handleDeleteSource(source.id)}
                            disabled={syncingId !== null || syncingAll}
                          >
                            삭제
                          </button>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          ) : (
            <div className="admin-section">
              <form className="admin-form" onSubmit={handleAddTag}>
                <h4>새 태그 추가</h4>
                <div className="form-row">
                  <input
                    type="text"
                    placeholder="태그 이름"
                    value={newTagName}
                    onChange={(e) => setNewTagName(e.target.value)}
                  />
                  <button type="submit" className="btn btn-primary">
                    추가
                  </button>
                </div>
              </form>

              <div className="admin-list">
                <h4>등록된 태그 ({tags.length})</h4>
                {tags.length === 0 ? (
                  <p className="empty-message">등록된 태그가 없습니다.</p>
                ) : (
                  <ul>
                    {tags.map((tag) => (
                      <li key={tag.id}>
                        <div className="list-item-info">
                          <span>#{tag.name}</span>
                        </div>
                        <button
                          className="btn-delete"
                          onClick={() => handleDeleteTag(tag.id)}
                        >
                          삭제
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
