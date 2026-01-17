import { useState, useEffect } from 'react';
import type { Tag } from '../types';
import { tagApi, feedApi } from '../api/client';
import './TagSelectModal.css';

interface TagSelectModalProps {
  isOpen: boolean;
  feedId: number | null;
  currentTags: { id: number; name: string }[];
  onClose: () => void;
  onUpdate: () => void;
}

export function TagSelectModal({
  isOpen,
  feedId,
  currentTags,
  onClose,
  onUpdate,
}: TagSelectModalProps) {
  const [allTags, setAllTags] = useState<Tag[]>([]);
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>([]);
  const [newTagName, setNewTagName] = useState('');
  const [newTagNames, setNewTagNames] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isOpen) {
      fetchTags();
      setSelectedTagIds(currentTags.map((t) => t.id));
      setNewTagName('');
      setNewTagNames([]);
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

  const handleAddNewTag = () => {
    const trimmed = newTagName.trim();
    if (!trimmed) return;

    // 이미 존재하는 태그인지 확인
    const existingTag = allTags.find(
      (t) => t.name.toLowerCase() === trimmed.toLowerCase()
    );
    if (existingTag) {
      // 기존 태그면 선택에 추가
      if (!selectedTagIds.includes(existingTag.id)) {
        setSelectedTagIds((prev) => [...prev, existingTag.id]);
      }
    } else {
      // 새 태그면 newTagNames에 추가
      if (!newTagNames.includes(trimmed)) {
        setNewTagNames((prev) => [...prev, trimmed]);
      }
    }
    setNewTagName('');
  };

  const handleRemoveNewTag = (tagName: string) => {
    setNewTagNames((prev) => prev.filter((name) => name !== tagName));
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAddNewTag();
    }
  };

  const handleSave = async () => {
    if (!feedId) return;

    setSaving(true);
    try {
      await feedApi.updateTags(feedId, selectedTagIds, newTagNames);
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
          {/* 새 태그 입력 */}
          <div className="new-tag-input">
            <input
              type="text"
              placeholder="새 태그 입력 후 Enter"
              value={newTagName}
              onChange={(e) => setNewTagName(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={saving}
            />
            <button
              type="button"
              className="btn-add-new-tag"
              onClick={handleAddNewTag}
              disabled={!newTagName.trim() || saving}
            >
              추가
            </button>
          </div>

          {/* 새로 추가할 태그 목록 */}
          {newTagNames.length > 0 && (
            <div className="new-tags-list">
              {newTagNames.map((name) => (
                <span key={name} className="new-tag-chip">
                  #{name}
                  <button
                    type="button"
                    className="remove-new-tag"
                    onClick={() => handleRemoveNewTag(name)}
                  >
                    ×
                  </button>
                </span>
              ))}
            </div>
          )}

          {loading ? (
            <div className="tag-modal-loading">태그 로딩 중...</div>
          ) : allTags.length === 0 && newTagNames.length === 0 ? (
            <div className="tag-modal-empty">
              등록된 태그가 없습니다.
              <br />
              위 입력창에서 새 태그를 추가해주세요.
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
