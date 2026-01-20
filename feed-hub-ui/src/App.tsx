import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { AdminModal, AdminButton, AuthModal, UserMenu, QnaButton } from './components';
import { HomePage, QnaListPage, QnaWritePage, QnaDetailPage } from './pages';
import { authApi, tokenManager } from './api/client';
import type { User } from './types';
import './App.css';

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [isAdminOpen, setIsAdminOpen] = useState(false);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);

  // Check for existing auth token on mount
  useEffect(() => {
    const token = tokenManager.getToken();
    if (token) {
      authApi.getMe()
        .then((userData) => setUser(userData))
        .catch(() => {
          tokenManager.removeToken();
        });
    }
  }, []);

  const handleAuthSuccess = (userData: User, token: string) => {
    tokenManager.setToken(token);
    setUser(userData);
    setIsAuthModalOpen(false);
  };

  const handleLogout = () => {
    tokenManager.removeToken();
    setUser(null);
  };

  const handleLoginRequired = () => {
    setIsAuthModalOpen(true);
  };

  return (
    <BrowserRouter>
      <div className="app">
        <header className="app-header">
          <div className="header-left">
            <Link to="/" className="header-logo">
              <h1>FeedHub</h1>
              <p>RSS 피드를 한눈에</p>
            </Link>
          </div>
          <div className="header-right">
            {user ? (
              <UserMenu user={user} onLogout={handleLogout} />
            ) : (
              <button
                type="button"
                className="btn-login"
                onClick={() => setIsAuthModalOpen(true)}
              >
                로그인
              </button>
            )}
          </div>
        </header>

        <main className="app-main">
          <Routes>
            <Route
              path="/"
              element={
                <HomePage user={user} onLoginRequired={handleLoginRequired} />
              }
            />
            <Route
              path="/qna"
              element={<QnaListPage user={user} />}
            />
            <Route
              path="/qna/admin"
              element={<QnaListPage user={user} />}
            />
            <Route
              path="/qna/write"
              element={<QnaWritePage user={user} />}
            />
            <Route
              path="/qna/:id"
              element={<QnaDetailPage user={user} />}
            />
          </Routes>
        </main>

        {/* Floating buttons */}
        {user && <QnaButton />}
        {user?.role === 'ADMIN' && (
          <>
            <AdminButton onClick={() => setIsAdminOpen(true)} />
            <AdminModal isOpen={isAdminOpen} onClose={() => setIsAdminOpen(false)} />
          </>
        )}

        <AuthModal
          isOpen={isAuthModalOpen}
          onClose={() => setIsAuthModalOpen(false)}
          onAuthSuccess={handleAuthSuccess}
        />
      </div>
    </BrowserRouter>
  );
}

export default App;
