import React, { useEffect, useState } from 'react';
import Layout from '../Layout/Layout';
import api from '../../api';

const AgentApproval = () => {
  const [pendingUsers, setPendingUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');

  useEffect(() => {
    fetchPendingUsers();
  }, []);

  const fetchPendingUsers = async () => {
    try {
      const response = await api.get('/admin/pending-users');
      setPendingUsers(response.data);
    } catch (err) {
      console.error('Failed to fetch pending users:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleApproval = async (userId, action) => {
    try {
      await api.post(`/admin/approve-user/${userId}`, { action });
      setMessage(`User ${action === 'approve' ? 'approved' : 'rejected'} successfully!`);
      fetchPendingUsers();
    } catch (err) {
      setMessage('Failed to process approval. Please try again.');
    }
  };

  return (
    <Layout>
      <h4 className="mb-4">Agent Approvals</h4>

      {message && (
        <div className={`alert ${message.includes('success') ? 'alert-success' : 'alert-danger'}`}>
          {message}
        </div>
      )}

      <div className="card card-glass p-4">
        {loading ? (
          <div className="text-center py-4">
            <div className="spinner-border text-primary"></div>
          </div>
        ) : pendingUsers.length > 0 ? (
          <div className="table-responsive">
            <table className="table table-hover">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Registered On</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {pendingUsers.map(user => (
                  <tr key={user.id}>
                    <td><strong>{user.username}</strong></td>
                    <td>{user.email || 'N/A'}</td>
                    <td>{user.phone || 'N/A'}</td>
                    <td>{new Date(user.createdAt).toLocaleDateString()}</td>
                    <td>
                      <button 
                        className="btn btn-success btn-sm me-2"
                        onClick={() => handleApproval(user.id, 'approve')}
                      >
                        <i className="fas fa-check me-1"></i> Approve
                      </button>
                      <button 
                        className="btn btn-danger btn-sm"
                        onClick={() => handleApproval(user.id, 'reject')}
                      >
                        <i className="fas fa-times me-1"></i> Reject
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="text-center py-4">
            <i className="fas fa-check-circle text-success" style={{ fontSize: '48px' }}></i>
            <p className="mt-3">No pending approvals</p>
          </div>
        )}
      </div>
    </Layout>
  );
};

export default AgentApproval;
