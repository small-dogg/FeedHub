import { useState, useEffect, useRef } from 'react';
import type { RssSource, Tag, BlogType } from '../types';
import { rssSourceApi, tagApi } from '../api/client';
import './AdminModal.css';

interface AdminModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const BLOG_TYPES: BlogType[] = ['TISTORY', 'MEDIUM', 'VELOG', 'GITHUB_BLOG', 'UNKNOWN'];

export function AdminModal({ isOpen, onClose }: AdminModalProps) {
  const [activeTab, setActiveTab] = useState<'sources' | 'tags'>('sources');
  const [rssSources, setRssSources] = useState<RssSource[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(false);
  const [syncingId, setSyncingId] = useState<number | null>(null);
  const [syncingAll, setSyncingAll] = useState(false);
  const [crawlingId, setCrawlingId] = useState<number | null>(null);
  const [crawlingAll, setCrawlingAll] = useState(false);
  const [importing, setImporting] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [adding, setAdding] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Form states
  const [newRssUrl, setNewRssUrl] = useState('');
  const [newTagName, setNewTagName] = useState('');

  // Edit state
  const [editingSource, setEditingSource] = useState<RssSource | null>(null);
  const [editForm, setEditForm] = useState({
    blogName: '',
    author: '',
    siteUrl: '',
    crawlUrl: '',
    language: '',
    blogType: '' as BlogType | '',
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isOpen) {
      fetchData();
    }
  }, [isOpen]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const sourcesData = await rssSourceApi.getAll();
      setRssSources(sourcesData);
    } catch (error) {
      console.error('RSS 소스 로드 실패:', error);
    }

    try {
      const tagsData = await tagApi.getAll();
      setTags(tagsData);
    } catch (error) {
      console.error('태그 로드 실패 (로그인 필요):', error);
      setTags([]);
    }

    setLoading(false);
  };

  const handleAddSource = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRssUrl.trim()) {
      alert('RSS URL을 입력하세요.');
      return;
    }
    setAdding(true);
    try {
      await rssSourceApi.create(newRssUrl.trim());
      setNewRssUrl('');
      fetchData();
    } catch (error) {
      console.error('RSS 소스 추가 실패:', error);
      alert('RSS 소스 추가에 실패했습니다.\nRSS URL이 올바른지 확인해주세요.');
    } finally {
      setAdding(false);
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

  const handleEditSource = (source: RssSource) => {
    setEditingSource(source);
    setEditForm({
      blogName: source.blogName,
      author: source.author || '',
      siteUrl: source.siteUrl || '',
      crawlUrl: source.crawlUrl || '',
      language: source.language || '',
      blogType: source.blogType || '',
    });
  };

  const handleSaveEdit = async () => {
    if (!editingSource) return;
    setSaving(true);
    try {
      await rssSourceApi.update(editingSource.id, {
        blogName: editForm.blogName || undefined,
        author: editForm.author || undefined,
        siteUrl: editForm.siteUrl || undefined,
        crawlUrl: editForm.crawlUrl || undefined,
        language: editForm.language || undefined,
        blogType: editForm.blogType || undefined,
      });
      setEditingSource(null);
      fetchData();
    } catch (error) {
      console.error('RSS 소스 수정 실패:', error);
      alert('RSS 소스 수정에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleCancelEdit = () => {
    setEditingSource(null);
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

  const handleCrawl = async (id: number) => {
    setCrawlingId(id);
    try {
      const result = await rssSourceApi.crawl(id);
      if (result.requested) {
        alert(`크롤링 요청 완료: ${result.blogName} (${result.blogType})`);
      } else {
        alert(`크롤링 요청 실패: ${result.message || '알 수 없는 오류'}`);
      }
    } catch (error) {
      console.error('크롤링 요청 실패:', error);
      alert('크롤링 요청에 실패했습니다.');
    } finally {
      setCrawlingId(null);
    }
  };

  const handleCrawlAll = async () => {
    if (!confirm('모든 RSS 소스를 크롤링하시겠습니까?\n(지원되는 블로그 타입만 크롤링됩니다)')) return;
    setCrawlingAll(true);
    try {
      const results = await rssSourceApi.crawlAll();
      const requested = results.filter(r => r.requested).length;
      const skipped = results.filter(r => !r.requested).length;
      alert(`전체 크롤링 요청 완료:\n- 요청됨: ${requested}개\n- 건너뜀: ${skipped}개 (미지원 타입)`);
    } catch (error) {
      console.error('전체 크롤링 요청 실패:', error);
      alert('전체 크롤링 요청에 실패했습니다.');
    } finally {
      setCrawlingAll(false);
    }
  };

  const handleImportOpml = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setImporting(true);
    try {
      const result = await rssSourceApi.importOpml(file, true);
      alert(
        `OPML 가져오기 완료!\n` +
        `- 발견: ${result.totalFound}개\n` +
        `- 등록: ${result.imported}개\n` +
        `- 건너뜀: ${result.skipped}개`
      );
      fetchData();
    } catch (error) {
      console.error('OPML 가져오기 실패:', error);
      alert('OPML 파일 가져오기에 실패했습니다.');
    } finally {
      setImporting(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleExportOpml = async () => {
    setExporting(true);
    try {
      await rssSourceApi.exportOpml();
    } catch (error) {
      console.error('OPML 내보내기 실패:', error);
      alert('OPML 파일 내보내기에 실패했습니다.');
    } finally {
      setExporting(false);
    }
  };

  const isActionDisabled = syncingAll || syncingId !== null || crawlingAll || crawlingId !== null;

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
              <div className="opml-actions">
                <input
                  type="file"
                  ref={fileInputRef}
                  accept=".opml,.xml"
                  onChange={handleImportOpml}
                  style={{ display: 'none' }}
                />
                <button
                  type="button"
                  className="btn btn-opml-import"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={importing || exporting || isActionDisabled}
                >
                  {importing ? '가져오는 중...' : 'OPML 가져오기'}
                </button>
                <button
                  type="button"
                  className="btn btn-opml-export"
                  onClick={handleExportOpml}
                  disabled={importing || exporting || isActionDisabled || rssSources.length === 0}
                >
                  {exporting ? '내보내는 중...' : 'OPML 내보내기'}
                </button>
              </div>

              <form className="admin-form" onSubmit={handleAddSource}>
                <h4>새 RSS 소스 추가</h4>
                <div className="form-row">
                  <input
                    type="url"
                    placeholder="RSS URL을 입력하세요"
                    value={newRssUrl}
                    onChange={(e) => setNewRssUrl(e.target.value)}
                    disabled={adding}
                  />
                  <button type="submit" className="btn btn-primary" disabled={adding}>
                    {adding ? '추가 중...' : '추가'}
                  </button>
                </div>
                <p className="form-hint">RSS URL만 입력하면 블로그 정보가 자동으로 수집됩니다.</p>
              </form>

              <div className="admin-list">
                <div className="list-header">
                  <h4>등록된 RSS 소스 ({rssSources.length})</h4>
                  {rssSources.length > 0 && (
                    <div className="list-header-actions">
                      <button
                        className="btn btn-sync-all"
                        onClick={handleSyncAll}
                        disabled={isActionDisabled}
                      >
                        {syncingAll ? '동기화 중...' : '전체 동기화'}
                      </button>
                      <button
                        className="btn btn-crawl-all"
                        onClick={handleCrawlAll}
                        disabled={isActionDisabled}
                      >
                        {crawlingAll ? '크롤링 중...' : '전체 크롤링'}
                      </button>
                    </div>
                  )}
                </div>
                {rssSources.length === 0 ? (
                  <p className="empty-message">등록된 RSS 소스가 없습니다.</p>
                ) : (
                  <ul>
                    {rssSources.map((source) => (
                      <li key={source.id}>
                        {editingSource?.id === source.id ? (
                          <div className="edit-form">
                            <div className="edit-form-row">
                              <label>블로그 이름</label>
                              <input
                                type="text"
                                value={editForm.blogName}
                                onChange={(e) => setEditForm({ ...editForm, blogName: e.target.value })}
                              />
                            </div>
                            <div className="edit-form-row">
                              <label>작성자</label>
                              <input
                                type="text"
                                value={editForm.author}
                                onChange={(e) => setEditForm({ ...editForm, author: e.target.value })}
                              />
                            </div>
                            <div className="edit-form-row">
                              <label>사이트 URL</label>
                              <input
                                type="url"
                                value={editForm.siteUrl}
                                onChange={(e) => setEditForm({ ...editForm, siteUrl: e.target.value })}
                              />
                            </div>
                            <div className="edit-form-row">
                              <label>크롤링 URL</label>
                              <input
                                type="url"
                                value={editForm.crawlUrl}
                                onChange={(e) => setEditForm({ ...editForm, crawlUrl: e.target.value })}
                                placeholder="예: https://medium.com/watcha/all"
                              />
                            </div>
                            <div className="edit-form-row">
                              <label>언어</label>
                              <select
                                value={editForm.language}
                                onChange={(e) => setEditForm({ ...editForm, language: e.target.value })}
                              >
                                <option value="">자동</option>
                                <option value="ko">한국어</option>
                                <option value="en">영어</option>
                                <option value="ja">일본어</option>
                              </select>
                            </div>
                            <div className="edit-form-row">
                              <label>블로그 타입</label>
                              <select
                                value={editForm.blogType}
                                onChange={(e) => setEditForm({ ...editForm, blogType: e.target.value as BlogType | '' })}
                              >
                                <option value="">선택</option>
                                {BLOG_TYPES.map((type) => (
                                  <option key={type} value={type}>{type}</option>
                                ))}
                              </select>
                            </div>
                            <div className="edit-form-actions">
                              <button
                                type="button"
                                className="btn btn-primary"
                                onClick={handleSaveEdit}
                                disabled={saving}
                              >
                                {saving ? '저장 중...' : '저장'}
                              </button>
                              <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={handleCancelEdit}
                                disabled={saving}
                              >
                                취소
                              </button>
                            </div>
                          </div>
                        ) : (
                          <>
                            <div className="list-item-info">
                              <div className="list-item-title">
                                <strong>{source.blogName}</strong>
                                {source.blogType && (
                                  <span className={`blog-type-badge blog-type-${source.blogType.toLowerCase()}`}>
                                    {source.blogType}
                                  </span>
                                )}
                              </div>
                              <span className="list-item-url">{source.rssUrl}</span>
                              {source.lastSyncAt && (
                                <span className="list-item-sync-time">
                                  마지막 동기화: {new Date(source.lastSyncAt).toLocaleString('ko-KR')}
                                </span>
                              )}
                            </div>
                            <div className="list-item-actions">
                              <button
                                className="btn-edit"
                                onClick={() => handleEditSource(source)}
                                disabled={isActionDisabled}
                              >
                                수정
                              </button>
                              <button
                                className="btn-sync"
                                onClick={() => handleSync(source.id)}
                                disabled={isActionDisabled}
                              >
                                {syncingId === source.id ? '동기화 중...' : '동기화'}
                              </button>
                              <button
                                className="btn-crawl"
                                onClick={() => handleCrawl(source.id)}
                                disabled={isActionDisabled}
                              >
                                {crawlingId === source.id ? '크롤링 중...' : '크롤링'}
                              </button>
                              <button
                                className="btn-delete"
                                onClick={() => handleDeleteSource(source.id)}
                                disabled={isActionDisabled}
                              >
                                삭제
                              </button>
                            </div>
                          </>
                        )}
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
