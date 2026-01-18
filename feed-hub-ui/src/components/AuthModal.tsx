import { useState } from 'react';
import { authApi } from '../api/client';
import type { User } from '../types';
import './AuthModal.css';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAuthSuccess: (user: User, token: string) => void;
}

type AuthMode = 'signin' | 'signup';

export function AuthModal({ isOpen, onClose, onAuthSuccess }: AuthModalProps) {
  const [mode, setMode] = useState<AuthMode>('signin');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [nickname, setNickname] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [emailChecked, setEmailChecked] = useState(false);
  const [emailAvailable, setEmailAvailable] = useState(false);

  const resetForm = () => {
    setEmail('');
    setPassword('');
    setPasswordConfirm('');
    setNickname('');
    setError('');
    setEmailChecked(false);
    setEmailAvailable(false);
  };

  const handleModeChange = (newMode: AuthMode) => {
    setMode(newMode);
    resetForm();
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const handleCheckEmail = async () => {
    if (!email.trim()) {
      setError('이메일을 입력해주세요.');
      return;
    }

    try {
      const result = await authApi.checkEmail(email);
      setEmailChecked(true);
      setEmailAvailable(result.available);
      if (!result.available) {
        setError('이미 사용 중인 이메일입니다.');
      } else {
        setError('');
      }
    } catch {
      setError('이메일 확인 중 오류가 발생했습니다.');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (mode === 'signup') {
        if (!emailChecked || !emailAvailable) {
          setError('이메일 중복 확인을 해주세요.');
          setLoading(false);
          return;
        }
        if (password !== passwordConfirm) {
          setError('비밀번호가 일치하지 않습니다.');
          setLoading(false);
          return;
        }
        if (password.length < 8) {
          setError('비밀번호는 8자 이상이어야 합니다.');
          setLoading(false);
          return;
        }
        const response = await authApi.signUp({
          email,
          password,
          passwordConfirm,
          nickname,
        });
        onAuthSuccess(response.user, response.accessToken);
        handleClose();
      } else {
        const response = await authApi.signIn({ email, password });
        onAuthSuccess(response.user, response.accessToken);
        handleClose();
      }
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setError(axiosErr.response?.data?.message || '오류가 발생했습니다.');
      } else {
        setError('오류가 발생했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="auth-modal-overlay" onClick={handleClose}>
      <div className="auth-modal" onClick={(e) => e.stopPropagation()}>
        <button type="button" className="auth-modal-close" onClick={handleClose}>
          &times;
        </button>

        <div className="auth-modal-tabs">
          <button
            type="button"
            className={`auth-tab ${mode === 'signin' ? 'active' : ''}`}
            onClick={() => handleModeChange('signin')}
          >
            로그인
          </button>
          <button
            type="button"
            className={`auth-tab ${mode === 'signup' ? 'active' : ''}`}
            onClick={() => handleModeChange('signup')}
          >
            회원가입
          </button>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="email">이메일</label>
            <div className="email-input-group">
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setEmailChecked(false);
                  setEmailAvailable(false);
                }}
                placeholder="example@email.com"
                required
              />
              {mode === 'signup' && (
                <button
                  type="button"
                  className="btn-check-email"
                  onClick={handleCheckEmail}
                  disabled={!email.trim()}
                >
                  중복확인
                </button>
              )}
            </div>
            {mode === 'signup' && emailChecked && emailAvailable && (
              <span className="email-available">사용 가능한 이메일입니다.</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="password">비밀번호</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={mode === 'signup' ? '8자 이상 입력' : '비밀번호 입력'}
              required
            />
          </div>

          {mode === 'signup' && (
            <>
              <div className="form-group">
                <label htmlFor="passwordConfirm">비밀번호 확인</label>
                <input
                  type="password"
                  id="passwordConfirm"
                  value={passwordConfirm}
                  onChange={(e) => setPasswordConfirm(e.target.value)}
                  placeholder="비밀번호 재입력"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="nickname">닉네임</label>
                <input
                  type="text"
                  id="nickname"
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  placeholder="2~50자"
                  required
                />
              </div>
            </>
          )}

          {error && <div className="auth-error">{error}</div>}

          <button type="submit" className="btn-auth-submit" disabled={loading}>
            {loading ? '처리 중...' : mode === 'signin' ? '로그인' : '회원가입'}
          </button>
        </form>
      </div>
    </div>
  );
}
