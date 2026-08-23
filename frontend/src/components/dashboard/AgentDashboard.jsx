import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../Layout/Layout';
import { dashboardApi, leadApi, dispositionApi } from '../../api';
import { useAuth } from '../../context/AuthContext';

const AgentDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [dashboardData, setDashboardData] = useState({
    totalAllocated: 0,
    totalDispositions: 0,
    todayDispositions: 0,
    pendingLeads: 0,
    ptpCount: 0,
    paidCount: 0,
    callbackCount: 0,
    remainingLeads: 0
  });
  const [recentLeads, setRecentLeads] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const [dashboardRes, leadsRes] = await Promise.all([
        dashboardApi.getAgentDashboard(),
        leadApi.getMyAllocation()
      ]);
      setDashboardData(dashboardRes.data);
      setRecentLeads(leadsRes.data?.slice(0, 5) || []);
    } catch (err) {
      console.error('Failed to fetch dashboard data:', err);
    } finally {
      setLoading(false);
    }
  };

  const statsCards = [
    { title: 'Allocated Leads', value: dashboardData.totalAllocated, icon: 'fa-user-plus', color: '#667eea' },
    { title: 'Total Dispositions', value: dashboardData.totalDispositions, icon: 'fa-check-circle', color: '#43e97b' },
    { title: "Today's Dispositions", value: dashboardData.todayDispositions, icon: 'fa-calendar-day', color: '#4facfe' },
    { title: 'Pending Leads', value: dashboardData.pendingLeads, icon: 'fa-clock', color: '#fa709a' },
    { title: 'PTP Count', value: dashboardData.ptpCount, icon: 'fa-phone', color: '#f6d365' },
    { title: 'Paid Count', value: dashboardData.paidCount, icon: 'fa-money-bill-wave', color: '#43e97b' },
    { title: 'Callback Count', value: dashboardData.callbackCount, icon: 'fa-undo', color: '#4facfe' },
    { title: 'Remaining Leads', value: dashboardData.remainingLeads, icon: 'fa-hourglass-half', color: '#f093fb' },
  ];

  return (
    <Layout>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 className="mb-1">Agent Dashboard</h4>
          <p className="text-muted">Welcome back, {user?.username}!</p>
        </div>
        <div>
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
                <div className="icon" style={{ background: `${stat.color}20`, color: stat.color }}>
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
            <h6 className="mb-3">Recent Allocated Leads</h6>
            {loading ? (
              <div className="text-center py-4">
                <div className="spinner-border text-primary"></div>
              </div>
            ) : recentLeads.length > 0 ? (
              <div className="table-responsive">
                <table className="table table-hover">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Phone</th>
                      <th>Lender</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentLeads.map((lead) => (
                      <tr key={lead.id}>
                        <td>{lead.name}</td>
                        <td>{lead.phoneNumber}</td>
                        <td>{lead.lenderProcessName || 'N/A'}</td>
                        <td>
                          <span className={`badge bg-${lead.status === 'Pending' ? 'warning' : 'success'}`}>
                            {lead.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-muted text-center py-4">No leads allocated yet</p>
            )}
          </div>
        </div>

        <div className="col-md-4">
          <div className="card card-glass p-3">
            <h6 className="mb-3">Quick Actions</h6>
            <div className="d-grid gap-2">
              <button className="btn btn-primary" onClick={() => navigate('/search')}>
                <i className="fas fa-search me-2"></i>Search Leads
              </button>
              <button className="btn btn-success" onClick={() => navigate('/dispose')}>
                <i className="fas fa-trash-alt me-2"></i>Dispose Lead
              </button>
              <button className="btn btn-info text-white" onClick={() => navigate('/profile')}>
                <i className="fas fa-user me-2"></i>My Profile
              </button>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default AgentDashboard;
