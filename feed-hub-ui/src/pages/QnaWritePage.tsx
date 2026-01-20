import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { qnaApi } from '../api/client';
import type { User, QnaType } from '../types';
import './QnaWritePage.css';

interface QnaWritePageProps {
  user: User | null;
}

const QNA_TYPES: { value: QnaType; label: string }[] = [
  { value: 'GENERAL', label: '일반문의' },
  { value: 'FEATURE_REQUEST', label: '기능요청' },
  { value: 'BUG_REPORT', label: '버그신고' },
  { value: 'RSS_REQUEST', label: 'RSS 추가요청' },
];

export function QnaWritePage({ user }: QnaWritePageProps) {
  const navigate = useNavigate();
  const [type, setType] = useState<QnaType>('GENERAL');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [isSecret, setIsSecret] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!user) {
      setError('로그인이 필요합니다.');
      return;
    }

    if (!title.trim()) {
      setError('제목을 입력해주세요.');
      return;
    }

    if (!content.trim()) {
      setError('내용을 입력해주세요.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await qnaApi.create({
        type,
        title: title.trim(),
        content: content.trim(),
        isSecret,
      });
      navigate('/qna');
    } catch (err) {
      console.error('QnA 등록 실패:', err);
      setError('QnA 등록에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    navigate(-1);
  };

  if (!user) {
    return (
      <div className="qna-write-page">
        <div className="qna-write-error">로그인이 필요합니다.</div>
      </div>
    );
  }

  return (
    <div className="qna-write-page">
      <h2>문의하기</h2>

      <form onSubmit={handleSubmit} className="qna-write-form">
        {error && <div className="form-error">{error}</div>}

        <div className="form-group">
          <label htmlFor="qna-type">문의 유형</label>
          <select
            id="qna-type"
            value={type}
            onChange={(e) => setType(e.target.value as QnaType)}
          >
            {QNA_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label htmlFor="qna-title">제목</label>
          <input
            id="qna-title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="제목을 입력하세요"
            maxLength={200}
          />
        </div>

        <div className="form-group">
          <label htmlFor="qna-content">내용</label>
          <textarea
            id="qna-content"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="문의 내용을 입력하세요"
            rows={10}
          />
        </div>

        <div className="form-group form-group-checkbox">
          <label>
            <input
              type="checkbox"
              checked={isSecret}
              onChange={(e) => setIsSecret(e.target.checked)}
            />
            <span>비밀글로 작성</span>
          </label>
          <p className="form-hint">비밀글은 본인과 관리자만 볼 수 있습니다.</p>
        </div>

        <div className="form-actions">
          <button
            type="button"
            className="btn-cancel"
            onClick={handleCancel}
            disabled={submitting}
          >
            취소
          </button>
          <button
            type="submit"
            className="btn-submit"
            disabled={submitting}
          >
            {submitting ? '등록 중...' : '등록'}
          </button>
        </div>
      </form>
    </div>
  );
}
