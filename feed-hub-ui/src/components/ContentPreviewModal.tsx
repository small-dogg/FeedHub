import { useEffect } from 'react';
import './ContentPreviewModal.css';

interface ContentPreviewModalProps {
  isOpen: boolean;
  title: string;
  content: string | null;
  link: string;
  onClose: () => void;
}

export function ContentPreviewModal({
  isOpen,
  title,
  content,
  link,
  onClose,
}: ContentPreviewModalProps) {
  // ESC 키로 모달 닫기
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // 모달 열릴 때 body 스크롤 방지
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div className="preview-modal-overlay" onClick={handleOverlayClick}>
      <div className="preview-modal">
        <button
          type="button"
          className="preview-modal-close"
          onClick={onClose}
          aria-label="닫기"
        >
          ×
        </button>

        <h2 className="preview-modal-title">{title}</h2>

        <div className="preview-modal-content">
          {content ? (
            <div dangerouslySetInnerHTML={{ __html: content }} />
          ) : (
            <p className="preview-modal-empty">미리보기 내용이 없습니다.</p>
          )}
        </div>

        <div className="preview-modal-footer">
          <a
            href={link}
            target="_blank"
            rel="noopener noreferrer"
            className="btn-view-original"
          >
            원문보기
          </a>
        </div>
      </div>
    </div>
  );
}
