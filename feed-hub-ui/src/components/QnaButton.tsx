import { useNavigate } from 'react-router-dom';
import './QnaButton.css';

export function QnaButton() {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate('/qna/write');
  };

  return (
    <button
      type="button"
      className="qna-button"
      onClick={handleClick}
      aria-label="문의하기"
      title="문의하기"
    >
      <svg
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <circle cx="12" cy="12" r="10" />
        <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
        <line x1="12" y1="17" x2="12.01" y2="17" />
      </svg>
    </button>
  );
}
