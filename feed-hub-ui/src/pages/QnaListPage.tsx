import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { qnaApi } from '../api/client';
import type { Qna, User, QnaType, QnaStatus } from '../types';
import './QnaListPage.css';

interface QnaListPageProps {
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

export function QnaListPage({ user }: QnaListPageProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const isAdminView = location.pathname === '/qna/admin';

  const [qnaList, setQnaList] = useState<Qna[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchQnaList = async () => {
      if (!user) {
        setError('로그인이 필요합니다.');
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);

      try {
        const data = isAdminView && user.role === 'ADMIN'
          ? await qnaApi.getAllList()
          : await qnaApi.getMyList();
        setQnaList(data);
      } catch (err) {
        console.error('QnA 목록 로드 실패:', err);
        setError('QnA 목록을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchQnaList();
  }, [user, isAdminView]);

  const handleQnaClick = (qnaId: number) => {
    navigate(`/qna/${qnaId}`);
  };

  const handleWriteClick = () => {
    navigate('/qna/write');
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  };

  if (loading) {
    return (
      <div className="qna-list-page">
        <div className="qna-loading">로딩 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="qna-list-page">
        <div className="qna-error">{error}</div>
      </div>
    );
  }

  return (
    <div className="qna-list-page">
      <div className="qna-list-header">
        <h2>{isAdminView ? '전체 QnA 관리' : '내 QnA'}</h2>
        <button type="button" className="btn-write-qna" onClick={handleWriteClick}>
          문의하기
        </button>
      </div>

      {user?.role === 'ADMIN' && (
        <div className="qna-view-toggle">
          <button
            type="button"
            className={!isAdminView ? 'active' : ''}
            onClick={() => navigate('/qna')}
          >
            내 QnA
          </button>
          <button
            type="button"
            className={isAdminView ? 'active' : ''}
            onClick={() => navigate('/qna/admin')}
          >
            전체 QnA
          </button>
        </div>
      )}

      {qnaList.length === 0 ? (
        <div className="qna-empty">
          <p>등록된 QnA가 없습니다.</p>
        </div>
      ) : (
        <div className="qna-list">
          <div className="qna-list-table">
            <div className="qna-table-header">
              <span className="col-status">상태</span>
              <span className="col-type">유형</span>
              <span className="col-title">제목</span>
              <span className="col-author">작성자</span>
              <span className="col-date">작성일</span>
              <span className="col-answers">답변</span>
            </div>
            {qnaList.map((qna) => (
              <div
                key={qna.id}
                className="qna-table-row"
                onClick={() => handleQnaClick(qna.id)}
              >
                <span className={`col-status status-${qna.status.toLowerCase()}`}>
                  {QNA_STATUS_LABELS[qna.status]}
                </span>
                <span className="col-type">{QNA_TYPE_LABELS[qna.type]}</span>
                <span className="col-title">
                  {qna.isSecret && <span className="secret-badge">비밀</span>}
                  {qna.title}
                </span>
                <span className="col-author">{qna.memberNickname}</span>
                <span className="col-date">{formatDate(qna.createdAt)}</span>
                <span className="col-answers">{qna.answerCount}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
