import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { qnaApi } from '../api/client';
import type { QnaDetail, User, QnaType, QnaStatus } from '../types';
import './QnaDetailPage.css';

interface QnaDetailPageProps {
  user: User | null;
}

const QNA_TYPE_LABELS: Record<QnaType, string> = {
  GENERAL: '일반문의',
  FEATURE_REQUEST: '기능요청',
  BUG_REPORT: '버그신고',
  RSS_REQUEST: 'RSS추가요청',
};

const QNA_STATUS_LABELS: Record<QnaStatus, string> = {
  PENDING: '답변대기',
  ANSWERED: '답변완료',
  CLOSED: '종료',
};

export function QnaDetailPage({ user }: QnaDetailPageProps) {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [qna, setQna] = useState<QnaDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Answer form state (admin only)
  const [answerContent, setAnswerContent] = useState('');
  const [submittingAnswer, setSubmittingAnswer] = useState(false);

  // Delete state
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    const fetchQna = async () => {
      if (!id || !user) {
        setError('로그인이 필요합니다.');
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);

      try {
        const data = await qnaApi.getDetail(Number(id));
        setQna(data);
      } catch (err) {
        console.error('QnA 로드 실패:', err);
        setError('QnA를 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchQna();
  }, [id, user]);

  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const handleAnswerSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!answerContent.trim() || !qna) return;

    setSubmittingAnswer(true);

    try {
      const newAnswer = await qnaApi.createAnswer(qna.id, {
        content: answerContent.trim(),
      });

      setQna({
        ...qna,
        status: 'ANSWERED',
        answers: [...qna.answers, newAnswer],
      });
      setAnswerContent('');
    } catch (err) {
      console.error('답변 등록 실패:', err);
      alert('답변 등록에 실패했습니다.');
    } finally {
      setSubmittingAnswer(false);
    }
  };

  const handleDelete = async () => {
    if (!qna || !confirm('정말 삭제하시겠습니까?')) return;

    setDeleting(true);

    try {
      await qnaApi.delete(qna.id);
      navigate('/qna');
    } catch (err) {
      console.error('QnA 삭제 실패:', err);
      alert('삭제에 실패했습니다.');
    } finally {
      setDeleting(false);
    }
  };

  const handleBack = () => {
    navigate('/qna');
  };

  if (loading) {
    return (
      <div className="qna-detail-page">
        <div className="qna-loading">로딩 중...</div>
      </div>
    );
  }

  if (error || !qna) {
    return (
      <div className="qna-detail-page">
        <div className="qna-error">{error || 'QnA를 찾을 수 없습니다.'}</div>
        <button type="button" className="btn-back" onClick={handleBack}>
          목록으로
        </button>
      </div>
    );
  }

  const isOwner = user?.nickname === qna.memberNickname;
  const isAdmin = user?.role === 'ADMIN';

  return (
    <div className="qna-detail-page">
      <button type="button" className="btn-back" onClick={handleBack}>
        &larr; 목록으로
      </button>

      <div className="qna-detail-card">
        <div className="qna-detail-header">
          <div className="qna-meta">
            <span className={`qna-status status-${qna.status.toLowerCase()}`}>
              {QNA_STATUS_LABELS[qna.status]}
            </span>
            <span className="qna-type">{QNA_TYPE_LABELS[qna.type]}</span>
            {qna.isSecret && <span className="qna-secret">비밀글</span>}
          </div>
          <h2 className="qna-title">{qna.title}</h2>
          <div className="qna-info">
            <span className="qna-author">{qna.memberNickname}</span>
            <span className="qna-date">{formatDateTime(qna.createdAt)}</span>
          </div>
        </div>

        <div className="qna-detail-content">
          <pre>{qna.content}</pre>
        </div>

        {isOwner && qna.answers.length === 0 && (
          <div className="qna-actions">
            <button
              type="button"
              className="btn-delete"
              onClick={handleDelete}
              disabled={deleting}
            >
              {deleting ? '삭제 중...' : '삭제'}
            </button>
          </div>
        )}
      </div>

      {qna.answers.length > 0 && (
        <div className="qna-answers">
          <h3>답변 ({qna.answers.length})</h3>
          {qna.answers.map((answer) => (
            <div key={answer.id} className="answer-card">
              <div className="answer-header">
                <span className="answer-author">
                  <span className="admin-badge">관리자</span>
                  {answer.memberNickname}
                </span>
                <span className="answer-date">{formatDateTime(answer.createdAt)}</span>
              </div>
              <div className="answer-content">
                <pre>{answer.content}</pre>
              </div>
            </div>
          ))}
        </div>
      )}

      {isAdmin && (
        <div className="answer-form-section">
          <h3>답변 작성</h3>
          <form onSubmit={handleAnswerSubmit} className="answer-form">
            <textarea
              value={answerContent}
              onChange={(e) => setAnswerContent(e.target.value)}
              placeholder="답변 내용을 입력하세요"
              rows={6}
            />
            <div className="form-actions">
              <button
                type="submit"
                className="btn-submit-answer"
                disabled={submittingAnswer || !answerContent.trim()}
              >
                {submittingAnswer ? '등록 중...' : '답변 등록'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
