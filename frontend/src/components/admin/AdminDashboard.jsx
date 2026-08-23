import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../Layout/Layout';
import { adminApi, leadApi, dispositionApi } from '../../api';
import { useAuth } from '../../context/AuthContext';

const AdminDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalAdmins: 0,
    totalAgents: 0,
    totalLeads: 0,
    pendingLeads: 0,
    disposedLeads: 0,
    pendingApprovals: 0,
    totalDispositions: 0
  });
  const [recentActivity, setRecentActivity] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    fetchAdminData();
  }, [retryCount]);

  const fetchAdminData = async () => {
    setLoading(true);
    setError('');
    try {
      console.log('Fetching admin dashboard data...');
      
      // Try to fetch stats
      let statsData = {};
      try {
        const statsRes = await adminApi.getStats();
        statsData = statsRes.data || {};
        console.log('Stats fetched:', statsData);
      } catch (statsErr) {
        console.error('Failed to fetch stats:', statsErr);
        // Use empty stats
      }

      // Try to fetch recent activity
      let activityData = [];
      try {
        const activityRes = await adminApi.getRecentActivity();
        activityData = activityRes.data || [];
        console.log('Activity fetched:', activityData);
      } catch (activityErr) {
        console.error('Failed to fetch recent activity:', activityErr);
      }

      // Try to fetch dispositions
      let dispositions = [];
      try {
        const dispositionsRes = await dispositionApi.getAllDispositions();
        dispositions = dispositionsRes.data || [];
        console.log('Dispositions fetched:', dispositions.length);
      } catch (dispErr) {
        console.error('Failed to fetch dispositions:', dispErr);
      }

      setStats({
        totalUsers: statsData.totalUsers || 0,
        totalAdmins: statsData.totalAdmins || 0,
        totalAgents: statsData.totalAgents || 0,
        totalLeads: statsData.totalLeads || 0,
        pendingLeads: statsData.pendingLeads || 0,
        disposedLeads: statsData.disposedLeads || 0,
        pendingApprovals: statsData.pendingApprovals || 0,
        totalDispositions: dispositions.length || 0
      });

      setRecentActivity(activityData);
      
      if (Object.keys(statsData).length === 0 && activityData.length === 0 && dispositions.length === 0) {
        setError('Unable to load dashboard data. Please check if the backend server is running.');
      }

    } catch (err) {
      console.error('Failed to fetch admin data:', err);
      setError('Failed to load dashboard data. Please check if the backend server is running.');
    } finally {
      setLoading(false);
    }
  };

  const handleRetry = () => {
    setRetryCount(prev => prev + 1);
  };

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

  const statsCards = [
    { title: 'Total Users', value: stats.totalUsers, icon: 'fa-users', color: '#4A6CF7' },
    { title: 'Admins', value: stats.totalAdmins, icon: 'fa-user-shield', color: '#D4A017' },
    { title: 'Agents', value: stats.totalAgents, icon: 'fa-user-tie', color: '#2D9B7A' },
    { title: 'Total Leads', value: stats.totalLeads, icon: 'fa-file-alt', color: '#5C7CFA' },
    { title: 'Pending Leads', value: stats.pendingLeads, icon: 'fa-clock', color: '#D45A5A' },
    { title: 'Disposed Leads', value: stats.disposedLeads, icon: 'fa-check-circle', color: '#3A8A8A' },
    { title: 'Pending Approvals', value: stats.pendingApprovals, icon: 'fa-user-check', color: '#D4A017' },
    { title: 'Dispositions', value: stats.totalDispositions, icon: 'fa-chart-bar', color: '#4A6CF7' },
  ];

  return (
    <Layout>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 className="mb-1">Admin Dashboard</h4>
          <p className="text-muted">Welcome back, {user?.username}!</p>
        </div>
        <div>
          <button 
            className="btn btn-sm btn-outline-secondary me-2"
            onClick={handleRetry}
            disabled={loading}
          >
            <i className={`fas fa-sync-alt ${loading ? 'fa-spin' : ''}`}></i>
          </button>
          <span className="badge bg-warning text-dark me-2">
            <i className="fas fa-shield-alt me-1"></i>
            Admin
          </span>
          <span className="text-muted">{new Date().toLocaleDateString()}</span>
        </div>
      </div>

      {error && (
        <div className="alert alert-warning">
          <i className="fas fa-exclamation-triangle me-2"></i>
          {error}
          <button className="btn btn-sm btn-primary ms-3" onClick={handleRetry}>
            <i className="fas fa-sync-alt me-1"></i> Retry
          </button>
        </div>
      )}

      <div className="row g-4 mb-4">
        {statsCards.map((stat, index) => (
          <div className="col-md-3 col-sm-6" key={index}>
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
            <h6 className="mb-3">Recent Activity</h6>
            {loading ? (
              <div className="text-center py-4">
                <div className="spinner-border text-primary"></div>
              </div>
            ) : recentActivity.length > 0 ? (
              <div className="list-group list-group-flush">
                {recentActivity.map((activity, index) => (
                  <div key={index} className="list-group-item d-flex justify-content-between align-items-center">
                    <div>
                      <i className={`fas ${activity.icon || 'fa-circle'} me-2 text-primary`}></i>
                      {activity.message || 'Activity'}
                    </div>
                    <small className="text-muted">{formatDate(activity.time)}</small>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-muted text-center py-4">No recent activity</p>
            )}
          </div>
        </div>

        <div className="col-md-4">
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
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default AdminDashboard;
