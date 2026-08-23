import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const Sidebar = ({ isOpen, toggleSidebar }) => {
  const { user, isAdmin, isSuperAdmin, logout } = useAuth();

  const userLinks = [
    { path: '/dashboard', icon: 'fa-th-large', label: 'Dashboard' },
    { path: '/search', icon: 'fa-search', label: 'Search' },
    { path: '/dispose', icon: 'fa-plus', label: 'Disposition' },
    { path: '/profile', icon: 'fa-user', label: 'Profile' },
    { path: '/reports', icon: 'fa-chart-bar', label: 'Reports' },
  ];

  const adminLinks = [
    { path: '/admin', icon: 'fa-cog', label: 'Admin Panel' },
    { path: '/admin/add-lead', icon: 'fa-upload', label: 'Upload Leads' },
    { path: '/admin/approvals', icon: 'fa-user-check', label: 'Approvals' },
    { path: '/admin/disposition-download', icon: 'fa-download', label: 'Download CSV' },
    { path: '/admin/teams', icon: 'fa-users', label: 'Teams' },
  ];

  const superAdminLinks = [
    { path: '/admin/users', icon: 'fa-user-shield', label: 'User Management' },
  ];

  const handleLogout = async () => {
    await logout();
  };

  return (
    <div className={`sidebar ${isOpen ? 'open' : ''}`}>
      <div className="sidebar-brand">
        <div className="logo-icon">
          <i className="fas fa-cubes"></i>
        </div>
        <h3>Master CRM</h3>
        <span className="version">v2.0</span>
      </div>

      <nav className="sidebar-nav">
        <div className="nav-section">
          <span className="nav-section-label">Main</span>
          {userLinks.map(link => (
            <NavLink
              key={link.path}
              to={link.path}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              onClick={() => window.innerWidth <= 768 && toggleSidebar()}
            >
              <i className={`fas ${link.icon}`}></i>
              <span>{link.label}</span>
            </NavLink>
          ))}
        </div>

        {(isAdmin() || isSuperAdmin()) && (
          <div className="nav-section">
            <span className="nav-section-label">Admin</span>
            {adminLinks.map(link => (
              <NavLink
                key={link.path}
                to={link.path}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                onClick={() => window.innerWidth <= 768 && toggleSidebar()}
              >
                <i className={`fas ${link.icon}`}></i>
                <span>{link.label}</span>
              </NavLink>
            ))}
          </div>
        )}

        {isSuperAdmin() && (
          <div className="nav-section">
            <span className="nav-section-label">Super Admin</span>
            {superAdminLinks.map(link => (
              <NavLink
                key={link.path}
                to={link.path}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                onClick={() => window.innerWidth <= 768 && toggleSidebar()}
              >
                <i className={`fas ${link.icon}`}></i>
                <span>{link.label}</span>
              </NavLink>
            ))}
          </div>
        )}

        <div className="nav-footer">
          <div className="user-card">
            <div className="user-avatar">
              {user?.username?.charAt(0).toUpperCase()}
            </div>
            <div className="user-info">
              <div className="user-name">{user?.username}</div>
              <div className="user-role">
                {user?.role?.replace('ROLE_', '') || 'User'}
              </div>
            </div>
          </div>
          <button className="logout-btn" onClick={handleLogout}>
            <i className="fas fa-sign-out-alt"></i>
            <span>Logout</span>
          </button>
        </div>
      </nav>
    </div>
  );
};

export default Sidebar;
