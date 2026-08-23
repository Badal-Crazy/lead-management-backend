import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    const result = await login(username, password);
    if (result.success) {
      const user = result.data;
      const role = user.role || '';
      if (role === 'SUPER_ADMIN' || role === 'ROLE_SUPER_ADMIN' || 
          role === 'ADMIN' || role === 'ROLE_ADMIN') {
        navigate('/admin');
      } else {
        navigate('/dashboard');
      }
    } else {
      setError(result.error);
    }
    setLoading(false);
  };

  return (
    <div className="login-container">
      <div className="login-bg">
        <div className="login-bg-blob blob-1"></div>
        <div className="login-bg-blob blob-2"></div>
        <div className="login-bg-blob blob-3"></div>
      </div>

      <div className="login-card-wrapper">
        <div className="login-card">
          <div className="login-brand">
            <div className="login-logo">
              <i className="fas fa-cubes"></i>
            </div>
            <h1>Master CRM</h1>
            <p>Lead Management System</p>
          </div>

          {error && (
            <div className="login-error">
              <i className="fas fa-exclamation-circle"></i>
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="login-form">
            <div className="form-group">
              <label>Email / Username</label>
              <div className="input-wrapper">
                <i className="fas fa-envelope"></i>
                <input
                  type="text"
                  placeholder="Enter your email or username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label>Password</label>
              <div className="input-wrapper">
                <i className="fas fa-lock"></i>
                <input
                  type="password"
                  placeholder="Enter your password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            <button type="submit" className="login-btn" disabled={loading}>
              {loading ? (
                <><span className="spinner"></span>Logging in...</>
              ) : (
                'Login to Dashboard'
              )}
            </button>
          </form>

          <div className="login-footer">
            <p>
              Don't have an account? <Link to="/register">Register here</Link>
            </p>
            <small>Registration requires admin approval</small>
          </div>
        </div>
      </div>

      <style>{`
        .login-container {
          min-height: 100vh;
          min-height: 100dvh;
          width: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          background: linear-gradient(135deg, #0c0c1d 0%, #1a1a3e 50%, #2d1b69 100%);
          position: relative;
          overflow: hidden;
          font-family: var(--font-family);
          padding: 20px;
        }

        .login-bg {
          position: absolute;
          inset: 0;
          z-index: 0;
        }

        .login-bg-blob {
          position: absolute;
          border-radius: 50%;
          filter: blur(120px);
          opacity: 0.3;
        }

        .blob-1 {
          width: 500px;
          height: 500px;
          background: #667eea;
          top: -200px;
          right: -200px;
          animation: floatBlob 10s ease-in-out infinite;
        }

        .blob-2 {
          width: 400px;
          height: 400px;
          background: #764ba2;
          bottom: -150px;
          left: -150px;
          animation: floatBlob 12s ease-in-out infinite reverse;
        }

        @keyframes floatBlob {
          0%, 100% { transform: translate(0, 0) scale(1); }
          50% { transform: translate(40px, -40px) scale(1.1); }
        }

        .login-card-wrapper {
          position: relative;
          z-index: 1;
          width: 100%;
          max-width: 440px;
        }

        .login-card {
          background: rgba(255, 255, 255, 0.08);
          backdrop-filter: blur(20px);
          border: 1px solid rgba(255, 255, 255, 0.12);
          border-radius: 24px;
          padding: 48px 40px;
          box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        }

        .login-brand {
          text-align: center;
          margin-bottom: 32px;
        }

        .login-logo {
          width: 72px;
          height: 72px;
          margin: 0 auto 16px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border-radius: 18px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 32px;
          color: white;
          box-shadow: 0 8px 32px rgba(102, 126, 234, 0.3);
        }

        .login-brand h1 {
          font-size: 28px;
          font-weight: 700;
          margin: 0;
          color: white;
          letter-spacing: -0.5px;
        }

        .login-brand p {
          color: rgba(255,255,255,0.6);
          font-size: 15px;
          margin: 6px 0 0;
        }

        .login-error {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 12px 16px;
          background: rgba(212, 90, 90, 0.15);
          border: 1px solid rgba(212, 90, 90, 0.2);
          border-radius: 10px;
          color: #ff6b6b;
          font-size: 14px;
          margin-bottom: 20px;
        }

        .login-form .form-group {
          margin-bottom: 20px;
        }

        .login-form label {
          display: block;
          font-size: 13px;
          font-weight: 600;
          color: rgba(255,255,255,0.8);
          margin-bottom: 6px;
        }

        .input-wrapper {
          position: relative;
        }

        .input-wrapper i {
          position: absolute;
          left: 14px;
          top: 50%;
          transform: translateY(-50%);
          color: rgba(255,255,255,0.4);
          font-size: 16px;
        }

        .input-wrapper input {
          width: 100%;
          padding: 12px 16px 12px 44px;
          background: rgba(255, 255, 255, 0.05);
          border: 1px solid rgba(255, 255, 255, 0.1);
          border-radius: 12px;
          color: white;
          font-size: 15px;
          transition: all 0.3s ease;
          font-family: var(--font-family);
        }

        .input-wrapper input:focus {
          outline: none;
          border-color: #667eea;
          box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
          background: rgba(255, 255, 255, 0.08);
        }

        .input-wrapper input::placeholder {
          color: rgba(255,255,255,0.3);
        }

        .login-btn {
          width: 100%;
          padding: 14px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border: none;
          border-radius: 12px;
          color: white;
          font-size: 16px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.3s ease;
          box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);
          font-family: var(--font-family);
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
        }

        .login-btn:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 30px rgba(102, 126, 234, 0.4);
        }

        .login-btn:disabled {
          opacity: 0.7;
          cursor: not-allowed;
          transform: none !important;
        }

        .login-btn .spinner {
          width: 18px;
          height: 18px;
          border: 2px solid rgba(255,255,255,0.2);
          border-top-color: white;
          border-radius: 50%;
          animation: spin 0.6s linear infinite;
        }

        @keyframes spin {
          to { transform: rotate(360deg); }
        }

        .login-footer {
          text-align: center;
          margin-top: 24px;
        }

        .login-footer p {
          color: rgba(255,255,255,0.6);
          font-size: 14px;
          margin: 0;
        }

        .login-footer a {
          color: #667eea;
          text-decoration: none;
          font-weight: 500;
        }

        .login-footer a:hover {
          color: #8b83ff;
        }

        .login-footer small {
          display: block;
          margin-top: 4px;
          color: rgba(255,255,255,0.3);
          font-size: 12px;
        }

        @media (max-width: 480px) {
          .login-card {
            padding: 32px 24px;
          }
          
          .login-brand h1 {
            font-size: 24px;
          }
          
          .login-logo {
            width: 56px;
            height: 56px;
            font-size: 24px;
          }
        }
      `}</style>
    </div>
  );
};

export default Login;
