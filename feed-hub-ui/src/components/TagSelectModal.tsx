import { useState, useEffect } from 'react';
import type { Tag } from '../types';
import { tagApi, rssSourceApi } from '../api/client';
import './TagSelectModal.css';

interface TagSelectModalProps {
  isOpen: boolean;
  rssSourceId: number | null;
  currentTags: { id: number; name: string }[];
  onClose: () => void;
  onUpdate: () => void;
}

export function TagSelectModal({
  isOpen,
  rssSourceId,
  currentTags,
  onClose,
  onUpdate,
}: TagSelectModalProps) {
  const [allTags, setAllTags] = useState<Tag[]>([]);
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isOpen) {
      fetchTags();
      setSelectedTagIds(currentTags.map((t) => t.id));
    }
  }, [isOpen, currentTags]);

  const fetchTags = async () => {
    setLoading(true);
    try {
      const tags = await tagApi.getAll();
      setAllTags(tags);
    } catch (error) {
      console.error('태그 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleTagToggle = (tagId: number) => {
    setSelectedTagIds((prev) =>
      prev.includes(tagId)
        ? prev.filter((id) => id !== tagId)
        : [...prev, tagId]
    );
  };

  const handleSave = async () => {
    if (!rssSourceId) return;

    setSaving(true);
    try {
      await rssSourceApi.updateTags(rssSourceId, selectedTagIds);
      onUpdate();
      onClose();
    } catch (error) {
      console.error('태그 업데이트 실패:', error);
      alert('태그 업데이트에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="tag-modal-overlay" onClick={onClose}>
      <div className="tag-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="tag-modal-header">
          <h3>태그 선택</h3>
          <button className="tag-modal-close" onClick={onClose}>
            &times;
          </button>
        </div>

        <div className="tag-modal-body">
          {loading ? (
            <div className="tag-modal-loading">태그 로딩 중...</div>
          ) : allTags.length === 0 ? (
            <div className="tag-modal-empty">
              등록된 태그가 없습니다.
              <br />
              관리자 설정에서 태그를 추가해주세요.
            </div>
          ) : (
            <div className="tag-select-list">
              {allTags.map((tag) => (
                <label key={tag.id} className="tag-select-item">
                  <input
                    type="checkbox"
                    checked={selectedTagIds.includes(tag.id)}
                    onChange={() => handleTagToggle(tag.id)}
                  />
                  <span className="tag-select-name">#{tag.name}</span>
                </label>
              ))}
            </div>
          )}
        </div>

        <div className="tag-modal-footer">
          <button
            type="button"
            className="btn btn-cancel"
            onClick={onClose}
            disabled={saving}
          >
            취소
          </button>
          <button
            type="button"
            className="btn btn-save"
            onClick={handleSave}
            disabled={saving || loading}
          >
            {saving ? '저장 중...' : '저장'}
          </button>
        </div>
      </div>
    </div>
  );
}
