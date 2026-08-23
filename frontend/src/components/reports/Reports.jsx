import React, { useState } from 'react';
import Layout from '../Layout/Layout';
import { reportsApi } from '../../api';
import { useAuth } from '../../context/AuthContext';

const Reports = () => {
  const { isAdmin, isSuperAdmin } = useAuth();
  const [reportType, setReportType] = useState('agent-performance');
  const [dateRange, setDateRange] = useState({
    fromDate: '',
    toDate: ''
  });
  const [filters, setFilters] = useState({
    adminId: '',
    agentId: '',
    teamId: '',
    dispositionType: ''
  });
  const [reportData, setReportData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const reportTypes = [
    { value: 'agent-performance', label: 'Agent Performance' },
    { value: 'admin-performance', label: 'Admin Performance' },
    { value: 'team-performance', label: 'Team Performance' },
    { value: 'lead-status', label: 'Lead Status' },
    { value: 'disposition-summary', label: 'Disposition Summary' },
    { value: 'allocation-summary', label: 'Allocation Summary' },
    { value: 'login-logout', label: 'Login/Logout Activity' },
    { value: 'date-wise', label: 'Date Wise Report' },
  ];

  const handleGenerate = async () => {
    setLoading(true);
    setError('');
    try {
      const params = { ...dateRange, ...filters };
      let response;
      switch (reportType) {
        case 'agent-performance':
          response = await reportsApi.getAgentPerformance(params);
          break;
        case 'admin-performance':
          response = await reportsApi.getAdminPerformance(params);
          break;
        case 'team-performance':
          response = await reportsApi.getTeamPerformance(params);
          break;
        case 'lead-status':
          response = await reportsApi.getLeadStatus(params);
          break;
        case 'disposition-summary':
          response = await reportsApi.getDispositionSummary(params);
          break;
        case 'allocation-summary':
          response = await reportsApi.getAllocationSummary(params);
          break;
        case 'login-logout':
          response = await reportsApi.getLoginLogout(params);
          break;
        case 'date-wise':
          response = await reportsApi.getDateWiseReport(params);
          break;
        default:
          response = await reportsApi.getAgentPerformance(params);
      }
      setReportData(response.data);
    } catch (err) {
      setError('Failed to generate report');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async () => {
    try {
      const params = { ...dateRange, ...filters, reportType };
      const response = await reportsApi.exportReport(params);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${reportType}-report.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      setError('Failed to export report');
    }
  };

  const renderReportTable = () => {
    if (!reportData) return null;
    
    const dataArray = Array.isArray(reportData) ? reportData : reportData.data || [];
    if (dataArray.length === 0) return <p className="text-muted">No data available</p>;
    
    const headers = Object.keys(dataArray[0] || {});
    if (headers.length === 0) return <p className="text-muted">No data available</p>;

    return (
      <div className="table-responsive">
        <table className="table table-hover">
          <thead>
            <tr>
              {headers.map((header) => (
                <th key={header}>{header.replace(/_/g, ' ').toUpperCase()}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {dataArray.map((row, index) => (
              <tr key={index}>
                {headers.map((header) => (
                  <td key={header}>{row[header] || 'N/A'}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <Layout>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 className="mb-1">Reports</h4>
          <p className="text-muted">Generate and export reports</p>
        </div>
        <button 
          className="btn btn-success"
          onClick={handleExport}
          disabled={!reportData}
        >
          <i className="fas fa-download me-2"></i>Export CSV
        </button>
      </div>

      <div className="card card-glass p-4">
        <div className="row">
          <div className="col-md-4 mb-3">
            <label className="form-label">Report Type</label>
            <select 
              className="form-select" 
              value={reportType} 
              onChange={(e) => setReportType(e.target.value)}
            >
              {reportTypes.map((type) => (
                <option key={type.value} value={type.value}>{type.label}</option>
              ))}
            </select>
          </div>
          <div className="col-md-3 mb-3">
            <label className="form-label">From Date</label>
            <input 
              type="date" 
              className="form-control"
              value={dateRange.fromDate}
              onChange={(e) => setDateRange({ ...dateRange, fromDate: e.target.value })}
            />
          </div>
          <div className="col-md-3 mb-3">
            <label className="form-label">To Date</label>
            <input 
              type="date" 
              className="form-control"
              value={dateRange.toDate}
              onChange={(e) => setDateRange({ ...dateRange, toDate: e.target.value })}
            />
          </div>
          <div className="col-md-2 d-flex align-items-end mb-3">
            <button 
              className="btn btn-primary w-100"
              onClick={handleGenerate}
              disabled={loading}
            >
              {loading ? 'Generating...' : 'Generate'}
            </button>
          </div>
        </div>

        {(isAdmin() || isSuperAdmin()) && (
          <div className="row">
            <div className="col-md-4 mb-3">
              <label className="form-label">Admin ID</label>
              <input 
                type="text" 
                className="form-control"
                placeholder="Filter by Admin"
                value={filters.adminId}
                onChange={(e) => setFilters({ ...filters, adminId: e.target.value })}
              />
            </div>
            <div className="col-md-4 mb-3">
              <label className="form-label">Agent ID</label>
              <input 
                type="text" 
                className="form-control"
                placeholder="Filter by Agent"
                value={filters.agentId}
                onChange={(e) => setFilters({ ...filters, agentId: e.target.value })}
              />
            </div>
            <div className="col-md-4 mb-3">
              <label className="form-label">Team ID</label>
              <input 
                type="text" 
                className="form-control"
                placeholder="Filter by Team"
                value={filters.teamId}
                onChange={(e) => setFilters({ ...filters, teamId: e.target.value })}
              />
            </div>
          </div>
        )}

        {error && <div className="alert alert-danger mt-3">{error}</div>}

        {reportData && (
          <div className="mt-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h6>Report Results ({Array.isArray(reportData) ? reportData.length : (reportData.total || reportData.count || 0)} records)</h6>
              <span className="text-muted small">Generated at: {new Date().toLocaleString()}</span>
            </div>
            {renderReportTable()}
          </div>
        )}
      </div>
    </Layout>
  );
};

export default Reports;
