import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../Layout/Layout';
import { dashboardApi, leadApi, dispositionApi } from '../../api';
import { useAuth } from '../../context/AuthContext';

const Dashboard = () => {
  const { user, isAdmin, isSuperAdmin } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalLeads: 0,
    pendingLeads: 0,
    disposedLeads: 0,
    totalAgents: 0,
    totalDispositions: 0,
    todayDispositions: 0
  });
  const [recentActivity, setRecentActivity] = useState([]);
  const [recentLeads, setRecentLeads] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError('');
    try {
      const promises = [];
      
      promises.push(
        leadApi.getLeads().catch(err => {
          console.error('Failed to fetch leads:', err);
          return { data: [] };
        })
      );
      
      promises.push(
        dispositionApi.getAllDispositions().catch(err => {
          console.error('Failed to fetch dispositions:', err);
          return { data: [] };
        })
      );
      
      if (isAdmin() || isSuperAdmin()) {
        promises.push(
          dashboardApi.getAdminDashboard().catch(err => {
            console.error('Failed to fetch admin dashboard:', err);
            return { data: {} };
          })
        );
        promises.push(
          leadApi.getTeamLeads().catch(err => {
            console.error('Failed to fetch team leads:', err);
            return { data: [] };
          })
        );
      } else {
        promises.push(
          dashboardApi.getAgentDashboard().catch(err => {
            console.error('Failed to fetch agent dashboard:', err);
            return { data: {} };
          })
        );
        promises.push(
          leadApi.getMyAllocation().catch(err => {
            console.error('Failed to fetch my allocation:', err);
            return { data: [] };
          })
        );
      }

      const [leadsRes, dispositionsRes, dashboardRes, recentLeadsRes] = await Promise.all(promises);
      
      const leads = leadsRes.data || [];
      const dispositions = dispositionsRes.data || [];
      const dashboardData = dashboardRes.data || {};
      const recentLeadsData = recentLeadsRes.data || [];

      const totalLeads = leads.length || dashboardData.totalLeads || 0;
      const pendingLeads = leads.filter(l => l.status === 'Pending' || l.status === 'pending').length || dashboardData.pendingLeads || 0;
      const disposedLeads = leads.filter(l => l.status === 'Disposed' || l.status === 'disposed').length || dashboardData.disposedLeads || 0;
      const totalDispositions = dispositions.length || dashboardData.totalDispositions || 0;
      
      const today = new Date().toDateString();
      const todayDispositions = dispositions.filter(d => {
        if (!d.createdAt) return false;
        return new Date(d.createdAt).toDateString() === today;
      }).length || dashboardData.todayDispositions || 0;

      setStats({
        totalLeads,
        pendingLeads,
        disposedLeads,
        totalAgents: dashboardData.totalAgents || 0,
        totalDispositions,
        todayDispositions
      });

      const sortedDispositions = [...dispositions].sort((a, b) => {
        return new Date(b.createdAt) - new Date(a.createdAt);
      }).slice(0, 5);

      const activities = sortedDispositions.map(d => ({
        message: `Disposition completed for ${d.leadName || 'Lead'}`,
        icon: 'fa-check-circle',
        time: d.createdAt || new Date().toISOString()
      }));

      if (activities.length === 0) {
        activities.push({
          message: 'No recent activity',
          icon: 'fa-info-circle',
          time: new Date().toISOString()
        });
      }

      setRecentActivity(activities);

      const sortedLeads = [...leads].sort((a, b) => {
        return new Date(b.createdAt) - new Date(a.createdAt);
      }).slice(0, 5);
      setRecentLeads(sortedLeads);

    } catch (err) {
      console.error('Failed to fetch dashboard data:', err);
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const handleNavigate = (path) => {
    navigate(path);
  };

  const statsCards = [
    { title: 'Total Leads', value: stats.totalLeads, icon: 'fa-users', color: '#4A6CF7' },
    { title: 'Pending Leads', value: stats.pendingLeads, icon: 'fa-clock', color: '#D4A017' },
    { title: 'Disposed Leads', value: stats.disposedLeads, icon: 'fa-check-circle', color: '#2D9B7A' },
    { title: "Today's Dispositions", value: stats.todayDispositions, icon: 'fa-calendar-day', color: '#5C7CFA' },
  ];

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

  if (error) {
    return (
      <Layout>
        <div className="alert alert-danger">{error}</div>
        <button className="btn btn-primary" onClick={fetchDashboardData}>
          <i className="fas fa-sync-alt me-2"></i>Retry
        </button>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 className="mb-1">Dashboard</h4>
          <p className="text-muted">Welcome back, {user?.username}!</p>
        </div>
        <div>
          <button 
            className="btn btn-sm btn-outline-secondary me-2"
            onClick={fetchDashboardData}
            disabled={loading}
          >
            <i className={`fas fa-sync-alt ${loading ? 'fa-spin' : ''}`}></i>
          </button>
          <span className="badge bg-success me-2">
            <i className="fas fa-circle me-1" style={{ fontSize: '8px' }}></i>
            Online
          </span>
          <span className="text-muted">{new Date().toLocaleDateString()}</span>
        </div>
      </div>

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
                onClick={() => handleNavigate('/search')}
              >
                <i className="fas fa-search me-2"></i>Search Leads
              </button>
              <button 
                className="btn btn-success" 
                onClick={() => handleNavigate('/dispose')}
              >
                <i className="fas fa-plus me-2"></i>Dispose Lead
              </button>
              {(isAdmin() || isSuperAdmin()) && (
                <>
                  <button 
                    className="btn btn-warning" 
                    onClick={() => handleNavigate('/admin/add-lead')}
                  >
                    <i className="fas fa-plus-circle me-2"></i>Add Lead
                  </button>
                  <button 
                    className="btn btn-info text-white" 
                    onClick={() => handleNavigate('/admin/approvals')}
                  >
                    <i className="fas fa-user-check me-2"></i>Pending Approvals
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default Dashboard;
