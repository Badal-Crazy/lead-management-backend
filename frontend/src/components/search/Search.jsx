import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../Layout/Layout';
import { leadApi } from '../../api';

const Search = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [searchType, setSearchType] = useState('name');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searchDuration, setSearchDuration] = useState('');
  const [selectedLeads, setSelectedLeads] = useState([]);
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('');

  const searchTypes = [
    { value: 'name', label: 'Name', icon: 'fa-user' },
    { value: 'phone', label: 'Mobile Number', icon: 'fa-phone' },
    { value: 'userId', label: 'User ID', icon: 'fa-id-card' },
    { value: 'agreement', label: 'Agreement ID', icon: 'fa-file-contract' },
  ];

  const showMessage = (text, type = 'success') => {
    setMessage(text);
    setMessageType(type);
    setTimeout(() => {
      setMessage('');
      setMessageType('');
    }, 5000);
  };

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchTerm.trim()) {
      showMessage('Please enter a search term', 'warning');
      return;
    }

    setLoading(true);
    setError('');
    setSearchDuration('');
    setSelectedLeads([]);
    try {
      const response = await leadApi.searchLeads(searchTerm, searchType);
      if (response.data) {
        setResults(response.data.data || []);
        setSearchDuration(response.data.duration || '');
        if (response.data.count === 0) {
          showMessage('No results found', 'info');
        }
      }
    } catch (err) {
      console.error('Search error:', err);
      setError('Failed to search leads. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleView = (leadId) => {
    navigate(`/lead/${leadId}`);
  };

  const handleDispose = (leadId) => {
    navigate(`/dispose/${leadId}`);
  };

  const handleSelectLead = (leadId) => {
    setSelectedLeads(prev => {
      if (prev.includes(leadId)) {
        return prev.filter(id => id !== leadId);
      } else {
        return [...prev, leadId];
      }
    });
  };

  const handleSelectAll = () => {
    if (selectedLeads.length === results.length && results.length > 0) {
      setSelectedLeads([]);
    } else {
      setSelectedLeads(results.map(lead => lead.id));
    }
  };

  const handleDeleteSelected = async () => {
    if (selectedLeads.length === 0) {
      showMessage('Please select leads to delete', 'warning');
      return;
    }
    
    if (!window.confirm(`Delete ${selectedLeads.length} selected lead(s)?`)) return;
    
    try {
      if (selectedLeads.length === 1) {
        await leadApi.deleteLead(selectedLeads[0]);
      } else {
        await leadApi.bulkDeleteLeads(selectedLeads);
      }
      showMessage(`${selectedLeads.length} lead(s) deleted successfully`);
      setSelectedLeads([]);
      handleSearch(new Event('submit'));
    } catch (err) {
      showMessage('Failed to delete leads', 'danger');
    }
  };

  const handleDeleteSingle = async (leadId, leadName) => {
    if (!window.confirm(`Delete lead ${leadName}?`)) return;
    try {
      await leadApi.deleteLead(leadId);
      showMessage('Lead deleted successfully');
      handleSearch(new Event('submit'));
    } catch (err) {
      showMessage('Failed to delete lead', 'danger');
    }
  };

  const getSearchTypeLabel = () => {
    const found = searchTypes.find(t => t.value === searchType);
    return found ? found.label : 'Search';
  };

  const getPlaceholder = () => {
    const placeholders = {
      name: 'Enter name (e.g., John Doe)',
      phone: 'Enter mobile number (e.g., 9876543210)',
      userId: 'Enter User ID (e.g., USER001)',
      agreement: 'Enter Agreement ID (e.g., AGR001)'
    };
    return placeholders[searchType] || 'Search...';
  };

  const getIcon = () => {
    const found = searchTypes.find(t => t.value === searchType);
    return found ? found.icon : 'fa-search';
  };

  return (
    <Layout>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 className="mb-1">Search Leads</h4>
          <p className="text-muted">Search by Name, Mobile Number, User ID, or Agreement ID</p>
        </div>
        <div className="d-flex align-items-center gap-2">
          {searchDuration && (
            <span className="badge bg-success">
              <i className="fas fa-clock me-1"></i>
              {searchDuration}
            </span>
          )}
          {selectedLeads.length > 0 && (
            <button 
              className="btn btn-danger btn-sm"
              onClick={handleDeleteSelected}
            >
              <i className="fas fa-trash me-1"></i>
              Delete Selected ({selectedLeads.length})
            </button>
          )}
        </div>
      </div>

      {message && (
        <div className={`alert alert-${messageType} alert-dismissible fade show`}>
          <i className={`fas fa-${messageType === 'success' ? 'check-circle' : messageType === 'danger' ? 'exclamation-circle' : 'info-circle'} me-2`}></i>
          {message}
          <button type="button" className="btn-close" onClick={() => setMessage('')}></button>
        </div>
      )}

      <div className="card card-glass p-4">
        <form onSubmit={handleSearch}>
          <div className="row g-3">
            <div className="col-md-3">
              <label className="form-label">Search By</label>
              <select 
                className="form-select" 
                value={searchType} 
                onChange={(e) => {
                  setSearchType(e.target.value);
                  setSearchTerm('');
                  setResults([]);
                }}
              >
                {searchTypes.map((type) => (
                  <option key={type.value} value={type.value}>
                    {type.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-7">
              <label className="form-label">Search Term</label>
              <div className="input-group">
                <span className="input-group-text bg-light">
                  <i className={`fas ${getIcon()}`}></i>
                </span>
                <input
                  type="text"
                  className="form-control form-control-lg"
                  placeholder={getPlaceholder()}
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  required
                />
              </div>
            </div>
            <div className="col-md-2 d-flex align-items-end">
              <button type="submit" className="btn btn-primary btn-lg w-100" disabled={loading}>
                {loading ? (
                  <span className="spinner-border spinner-border-sm"></span>
                ) : (
                  <><i className="fas fa-search me-2"></i>Search</>
                )}
              </button>
            </div>
          </div>
        </form>

        {error && <div className="alert alert-danger mt-3">{error}</div>}

        {results.length > 0 && (
          <div className="mt-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <p className="text-muted mb-0">Found {results.length} results</p>
              <span className="badge bg-primary">
                <i className="fas fa-search me-1"></i>
                {getSearchTypeLabel()}
              </span>
            </div>
            <div className="table-responsive">
              <table className="table table-hover">
                <thead>
                  <tr>
                    <th style={{ width: '40px' }}>
                      <input 
                        type="checkbox" 
                        checked={selectedLeads.length === results.length && results.length > 0}
                        onChange={handleSelectAll}
                      />
                    </th>
                    <th>Name</th>
                    <th>Mobile</th>
                    <th>User ID</th>
                    <th>Agreement ID</th>
                    <th>Lender</th>
                    <th>Allocation Month</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {results.map((lead) => (
                    <tr key={lead.id}>
                      <td>
                        <input 
                          type="checkbox" 
                          checked={selectedLeads.includes(lead.id)}
                          onChange={() => handleSelectLead(lead.id)}
                        />
                      </td>
                      <td><strong>{lead.name || 'N/A'}</strong></td>
                      <td>{lead.phoneNumber || 'N/A'}</td>
                      <td>
                        <span className="badge bg-secondary">{lead.userId || 'N/A'}</span>
                      </td>
                      <td>
                        <span className="badge bg-primary">{lead.agreementNumber || 'N/A'}</span>
                      </td>
                      <td>{lead.lenderProcessName || 'N/A'}</td>
                      <td>{lead.allocationMonthYear || 'N/A'}</td>
                      <td>
                        <div className="d-flex gap-1">
                          <button 
                            className="btn btn-sm btn-info text-white"
                            onClick={() => handleView(lead.id)}
                            title="View Profile"
                          >
                            <i className="fas fa-eye"></i>
                          </button>
                          <button 
                            className="btn btn-sm btn-success"
                            onClick={() => handleDispose(lead.id)}
                            title="Dispose"
                          >
                            <i className="fas fa-plus"></i>
                          </button>
                          <button 
                            className="btn btn-sm btn-danger"
                            onClick={() => handleDeleteSingle(lead.id, lead.name)}
                            title="Delete"
                          >
                            <i className="fas fa-times"></i>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {results.length === 0 && searchTerm && !loading && (
          <div className="alert alert-info mt-3">
            <i className="fas fa-info-circle me-2"></i>
            No results found for "{searchTerm}" in {getSearchTypeLabel()}
          </div>
        )}
      </div>
    </Layout>
  );
};

export default Search;
