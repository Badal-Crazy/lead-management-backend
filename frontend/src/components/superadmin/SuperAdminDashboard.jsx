import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../Layout/Layout';
import { adminApi } from '../../api';
import { useAuth } from '../../context/AuthContext';

const SuperAdminDashboard = () => {
  const { user, logout, forceLogout, isSuperAdmin } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalAdmins: 0,
    totalAgents: 0,
    totalLeads: 0,
    totalDispositions: 0,
    pendingApprovals: 0
  });
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('');

  useEffect(() => {
    if (!isSuperAdmin()) {
      navigate('/dashboard');
      return;
    }
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [statsRes, usersRes] = await Promise.all([
        adminApi.getStats(),
        adminApi.getAllUsers()
      ]);
      setStats(statsRes.data);
      setUsers(usersRes.data || []);
    } catch (err) {
      console.error('Failed to fetch data:', err);
    } finally {
      setLoading(false);
    }
  };

  const showMessage = (text, type = 'success') => {
    setMessage(text);
    setMessageType(type);
    setTimeout(() => {
      setMessage('');
      setMessageType('');
    }, 5000);
  };

  // Format date to human-readable format
  const formatDate = (dateString) => {
    if (!dateString) return 'Just now';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return 'Just now';
      
      const now = new Date();
      const diffMs = now - date;
      const diffMins = Math.floor(diffMs / 60000);
      const diffHours = Math.floor(diffMs / 3600000);
      const diffDays = Math.floor(diffMs / 86400000);

      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return `${diffMins}m ago`;
      if (diffHours < 24) return `${diffHours}h ago`;
      if (diffDays < 7) return `${diffDays}d ago`;
      
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return 'Just now';
    }
  };

  const handleNavigate = (path) => {
    navigate(path);
  };

  const handleApproveUser = async (username) => {
    try {
      await adminApi.approveUser(username);
      showMessage(`User ${username} approved successfully`);
      fetchData();
    } catch (err) {
      showMessage('Failed to approve user', 'danger');
    }
  };

  const handleDeleteUser = async (username) => {
    if (!window.confirm(`Delete user ${username}?`)) return;
    try {
      await adminApi.deleteUser(username);
      showMessage(`User ${username} deleted successfully`);
      fetchData();
    } catch (err) {
      showMessage('Failed to delete user', 'danger');
    }
  };

  const handleChangePassword = async (username) => {
    const newPassword = prompt(`Enter new password for ${username}:`);
    if (newPassword && newPassword.length >= 6) {
      try {
        await adminApi.changePassword(username, newPassword);
        showMessage(`Password changed for ${username}`);
      } catch (err) {
        showMessage('Failed to change password', 'danger');
      }
    } else if (newPassword) {
      showMessage('Password must be at least 6 characters', 'danger');
    }
  };

  const handleToggleUser = async (username, enabled) => {
    try {
      await adminApi.toggleUser(username, !enabled);
      showMessage(`User ${username} ${!enabled ? 'activated' : 'deactivated'}`);
      fetchData();
    } catch (err) {
      showMessage('Failed to toggle user', 'danger');
    }
  };

  const handleForceLogout = async (username) => {
    if (!window.confirm(`Force logout ${username}?`)) return;
    const result = await forceLogout(username);
    if (result.success) {
      showMessage(`User ${username} logged out forcefully`);
    } else {
      showMessage('Failed to force logout', 'danger');
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const statsCards = [
    { title: 'Total Users', value: stats.totalUsers, icon: 'fa-users', color: '#4A6CF7' },
    { title: 'Admins', value: stats.totalAdmins, icon: 'fa-user-shield', color: '#D4A017' },
    { title: 'Agents', value: stats.totalAgents, icon: 'fa-user-tie', color: '#2D9B7A' },
    { title: 'Total Leads', value: stats.totalLeads, icon: 'fa-file-alt', color: '#5C7CFA' },
    { title: 'Dispositions', value: stats.totalDispositions, icon: 'fa-check-circle', color: '#3A8A8A' },
    { title: 'Pending Approvals', value: stats.pendingApprovals, icon: 'fa-clock', color: '#D45A5A' },
  ];

  return (
    <Layout>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 className="mb-1">Super Admin Dashboard</h4>
          <p className="text-muted">Full system control and monitoring</p>
        </div>
        <div>
          <button className="btn btn-outline-danger" onClick={handleLogout}>
            <i className="fas fa-sign-out-alt me-2"></i>Logout
          </button>
        </div>
      </div>

      {message && (
        <div className={`alert alert-${messageType} alert-dismissible`}>
          {message}
          <button type="button" className="btn-close" onClick={() => setMessage('')}></button>
        </div>
      )}

      <div className="row g-4 mb-4">
        {statsCards.map((stat, index) => (
          <div className="col-md-2 col-sm-4" key={index}>
            <div className="card stats-card">
              <div className="d-flex justify-content-between align-items-center">
                <div>
                  <h6 className="text-muted mb-1">{stat.title}</h6>
                  <h3 className="mb-0 fw-bold">{loading ? '...' : stat.value}</h3>
                </div>
                <div className="icon" style={{ background: `${stat.color}15`, color: stat.color }}>
                  <i className={`fas ${stat.icon}`}></i>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="row">
        <div className="col-md-8">
          <div className="card card-glass p-3">
            <h6 className="mb-3">Quick Actions</h6>
            <div className="d-grid gap-2">
              <button 
                className="btn btn-primary" 
                onClick={() => handleNavigate('/admin/add-lead')}
              >
                <i className="fas fa-plus-circle me-2"></i>Add Lead
              </button>
              <button 
                className="btn btn-success" 
                onClick={() => handleNavigate('/admin/approvals')}
              >
                <i className="fas fa-user-check me-2"></i>Pending Approvals
              </button>
              <button 
                className="btn btn-info text-white" 
                onClick={() => handleNavigate('/admin/disposition-download')}
              >
                <i className="fas fa-download me-2"></i>Download CSV
              </button>
              <button 
                className="btn btn-warning" 
                onClick={() => handleNavigate('/admin/users')}
              >
                <i className="fas fa-users-cog me-2"></i>User Management
              </button>
              <button 
                className="btn btn-danger" 
                onClick={() => handleNavigate('/admin/teams')}
              >
                <i className="fas fa-users me-2"></i>Team Management
              </button>
            </div>
          </div>
        </div>

        <div className="col-md-4">
          <div className="card card-glass p-3">
            <h6 className="mb-3">User Management</h6>
            <div className="table-responsive">
              <table className="table table-sm">
                <thead>
                  <tr>
                    <th>User</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.slice(0, 5).map(user => (
                    <tr key={user.username}>
                      <td>{user.username}</td>
                      <td>
                        <span className={`badge bg-${user.enabled ? 'success' : 'danger'}`}>
                          {user.enabled ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td>
                        <button 
                          className="btn btn-sm btn-outline-primary me-1"
                          onClick={() => handleChangePassword(user.username)}
                        >
                          <i className="fas fa-key"></i>
                        </button>
                        {user.username !== 'superadmin' && (
                          <button 
                            className="btn btn-sm btn-outline-danger"
                            onClick={() => handleDeleteUser(user.username)}
                          >
                            <i className="fas fa-trash"></i>
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default SuperAdminDashboard;
