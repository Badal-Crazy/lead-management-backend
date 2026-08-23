import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const Navbar = ({ toggleSidebar }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [showNotifications, setShowNotifications] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const getRoleBadge = () => {
    const role = user?.role || '';
    if (role === 'SUPER_ADMIN' || role === 'ROLE_SUPER_ADMIN') {
      return <span className="role-badge super-admin">Super Admin</span>;
    } else if (role === 'ADMIN' || role === 'ROLE_ADMIN') {
      return <span className="role-badge admin">Admin</span>;
    } else {
      return <span className="role-badge agent">Agent</span>;
    }
  };

  return (
    <header className="navbar-custom">
      <div className="navbar-left">
        <button className="menu-toggle" onClick={toggleSidebar}>
          <i className="fas fa-bars"></i>
        </button>
        <div className="navbar-greeting">
          <span className="greeting-text">Welcome back,</span>
          <span className="greeting-name">{user?.username}</span>
          {getRoleBadge()}
        </div>
      </div>

      <div className="navbar-right">
        <div className="navbar-actions">
          <button className="action-btn" onClick={() => navigate('/search')} title="Search">
            <i className="fas fa-search"></i>
          </button>
          <button className="action-btn" onClick={() => navigate('/dispose')} title="Dispose">
            <i className="fas fa-plus"></i>
          </button>
          <div className="notification-wrapper">
            <button 
              className="action-btn notification-btn"
              onClick={() => setShowNotifications(!showNotifications)}
              title="Notifications"
            >
              <i className="fas fa-bell"></i>
              <span className="notification-dot"></span>
            </button>
            {showNotifications && (
              <div className="notification-dropdown">
                <div className="notification-header">
                  <h6>Notifications</h6>
                  <button className="mark-all-read">Mark all read</button>
                </div>
                <div className="notification-list">
                  <div className="notification-item">
                    <div className="notification-icon">
                      <i className="fas fa-user-plus text-success"></i>
                    </div>
                    <div className="notification-content">
                      <p>New user registered</p>
                      <span>2 min ago</span>
                    </div>
                  </div>
                  <div className="notification-item">
                    <div className="notification-icon">
                      <i className="fas fa-file-upload text-primary"></i>
                    </div>
                    <div className="notification-content">
                      <p>CSV upload completed</p>
                      <span>1 hour ago</span>
                    </div>
                  </div>
                  <div className="notification-item">
                    <div className="notification-icon">
                      <i className="fas fa-check-circle text-success"></i>
                    </div>
                    <div className="notification-content">
                      <p>Lead disposition added</p>
                      <span>3 hours ago</span>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="user-dropdown">
          <button className="user-btn" data-bs-toggle="dropdown">
            <div className="user-avatar">
              {user?.username?.charAt(0).toUpperCase()}
            </div>
            <span className="user-name">{user?.username}</span>
            <i className="fas fa-chevron-down"></i>
          </button>
          <ul className="dropdown-menu">
            <li><a className="dropdown-item" href="/profile"><i className="fas fa-user me-2"></i>Profile</a></li>
            <li><a className="dropdown-item" href="/dashboard"><i className="fas fa-th-large me-2"></i>Dashboard</a></li>
            <li><hr className="dropdown-divider" /></li>
            <li><button className="dropdown-item text-danger" onClick={handleLogout}>
              <i className="fas fa-sign-out-alt me-2"></i>Logout
            </button></li>
          </ul>
        </div>
      </div>

      <style>{`
        .navbar-custom {
          position: sticky;
          top: 0;
          z-index: 998;
          padding: 12px 24px;
          background: #FFFFFF;
          border-bottom: 1px solid #E8E8F0;
          display: flex;
          justify-content: space-between;
          align-items: center;
          width: 100%;
        }

        .navbar-left {
          display: flex;
          align-items: center;
          gap: 16px;
        }

        .menu-toggle {
          display: none;
          background: none;
          border: none;
          color: #1A1A2E;
          font-size: 20px;
          cursor: pointer;
          padding: 4px;
        }

        .navbar-greeting {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;
        }

        .greeting-text {
          color: #8A8AAA;
          font-size: 14px;
        }

        .greeting-name {
          font-weight: 600;
          font-size: 14px;
          color: #1A1A2E;
        }

        .role-badge {
          padding: 2px 12px;
          border-radius: 50px;
          font-size: 10px;
          font-weight: 600;
          text-transform: uppercase;
        }

        .role-badge.super-admin {
          background: rgba(212, 90, 90, 0.08);
          color: #D45A5A;
        }

        .role-badge.admin {
          background: rgba(212, 160, 23, 0.08);
          color: #D4A017;
        }

        .role-badge.agent {
          background: rgba(45, 155, 122, 0.08);
          color: #2D9B7A;
        }

        .navbar-right {
          display: flex;
          align-items: center;
          gap: 16px;
        }

        .navbar-actions {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .action-btn {
          width: 36px;
          height: 36px;
          border-radius: 50%;
          border: none;
          background: #F5F7FA;
          color: #4A4A6A;
          cursor: pointer;
          transition: all 0.2s ease;
          position: relative;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .action-btn:hover {
          background: #E8E8F0;
          color: #1A1A2E;
        }

        .notification-dot {
          position: absolute;
          top: 6px;
          right: 6px;
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background: #D45A5A;
        }

        .notification-wrapper {
          position: relative;
        }

        .notification-dropdown {
          position: absolute;
          top: calc(100% + 8px);
          right: 0;
          width: 320px;
          background: #FFFFFF;
          border: 1px solid #E8E8F0;
          border-radius: 12px;
          padding: 12px;
          box-shadow: 0 8px 40px rgba(0, 0, 0, 0.1);
          z-index: 1001;
        }

        .notification-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding-bottom: 8px;
          border-bottom: 1px solid #E8E8F0;
        }

        .notification-header h6 {
          margin: 0;
          font-size: 14px;
          color: #1A1A2E;
        }

        .mark-all-read {
          background: none;
          border: none;
          color: #4A6CF7;
          font-size: 12px;
          cursor: pointer;
        }

        .notification-list {
          max-height: 300px;
          overflow-y: auto;
        }

        .notification-item {
          display: flex;
          gap: 12px;
          padding: 8px 0;
          border-bottom: 1px solid #F5F7FA;
        }

        .notification-item:last-child {
          border-bottom: none;
        }

        .notification-icon {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          background: #F5F7FA;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .notification-content {
          flex: 1;
        }

        .notification-content p {
          margin: 0;
          font-size: 13px;
          color: #1A1A2E;
        }

        .notification-content span {
          font-size: 11px;
          color: #8A8AAA;
        }

        .user-dropdown {
          position: relative;
        }

        .user-btn {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 4px 12px 4px 4px;
          border-radius: 50px;
          border: 1px solid #E8E8F0;
          background: #FFFFFF;
          color: #1A1A2E;
          cursor: pointer;
          transition: all 0.2s ease;
        }

        .user-btn:hover {
          background: #F5F7FA;
        }

        .user-btn .user-avatar {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          background: linear-gradient(135deg, #4A6CF7 0%, #2D4BA8 100%);
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 700;
          font-size: 13px;
          color: white;
        }

        .user-btn .user-name {
          font-size: 13px;
          font-weight: 500;
          color: #1A1A2E;
        }

        .user-btn .fa-chevron-down {
          font-size: 10px;
          color: #8A8AAA;
        }

        .dropdown-menu {
          position: absolute;
          top: calc(100% + 8px);
          right: 0;
          min-width: 180px;
          background: #FFFFFF;
          border: 1px solid #E8E8F0;
          border-radius: 12px;
          padding: 8px;
          box-shadow: 0 8px 40px rgba(0, 0, 0, 0.1);
          z-index: 1001;
        }

        .dropdown-item {
          display: flex;
          align-items: center;
          padding: 8px 12px;
          border-radius: 8px;
          color: #4A4A6A;
          text-decoration: none;
          cursor: pointer;
          transition: all 0.2s ease;
          font-size: 13px;
          background: none;
          border: none;
          width: 100%;
        }

        .dropdown-item:hover {
          background: #F5F7FA;
          color: #1A1A2E;
        }

        .dropdown-item.text-danger:hover {
          background: rgba(212, 90, 90, 0.06);
          color: #D45A5A;
        }

        @media (max-width: 992px) {
          .menu-toggle {
            display: block;
          }
          
          .navbar-greeting .greeting-text {
            display: none;
          }
        }

        @media (max-width: 576px) {
          .navbar-custom {
            padding: 8px 12px;
          }
          
          .user-btn .user-name {
            display: none;
          }
          
          .user-btn .fa-chevron-down {
            display: none;
          }
          
          .notification-dropdown {
            width: calc(100vw - 24px);
            right: -12px;
          }
        }
      `}</style>
    </header>
  );
};

export default Navbar;
