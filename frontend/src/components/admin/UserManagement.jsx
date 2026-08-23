import React, { useEffect, useState } from 'react';
import Layout from '../Layout/Layout';
import { userApi, adminApi } from '../../api';
import { useAuth } from '../../context/AuthContext';

const UserManagement = () => {
  const { user, isSuperAdmin } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newUser, setNewUser] = useState({
    username: '',
    password: '',
    email: '',
    phone: '',
    role: 'AGENT'
  });

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      let response;
      if (isSuperAdmin()) {
        response = await userApi.getAllUsers();
      } else {
        response = await userApi.getUsers();
      }
      setUsers(response.data || []);
    } catch (err) {
      console.error('Failed to fetch users:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateUser = async () => {
    if (!newUser.username || !newUser.password) {
      setMessage('Username and password are required');
      return;
    }
    try {
      await userApi.createUser(newUser);
      setMessage('User created successfully');
      setShowCreateForm(false);
      setNewUser({ username: '', password: '', email: '', phone: '', role: 'AGENT' });
      fetchUsers();
    } catch (err) {
      setMessage('Failed to create user');
    }
  };

  const handleDeleteUser = async (userId) => {
    if (!window.confirm(`Delete user ${userId}?`)) return;
    try {
      await userApi.deleteUser(userId);
      setMessage('User deleted successfully');
      fetchUsers();
    } catch (err) {
      setMessage('Failed to delete user');
    }
  };

  const handleToggleUser = async (userId, enabled) => {
    try {
      if (enabled) {
        await userApi.deactivateUser(userId);
      } else {
        await userApi.activateUser(userId);
      }
      setMessage(`User ${enabled ? 'deactivated' : 'activated'} successfully`);
      fetchUsers();
    } catch (err) {
      setMessage('Failed to toggle user');
    }
  };

  const handleUpdateRole = async (userId, role) => {
    try {
      await userApi.updateUserRole(userId, role);
      setMessage('User role updated successfully');
      fetchUsers();
    } catch (err) {
      setMessage('Failed to update role');
    }
  };

  const roleOptions = ['AGENT', 'ADMIN', 'SUPER_ADMIN'];

  return (
    <Layout>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 className="mb-1">User Management</h4>
          <p className="text-muted">Manage system users</p>
        </div>
        {isSuperAdmin() && (
          <button className="btn btn-primary" onClick={() => setShowCreateForm(!showCreateForm)}>
            <i className="fas fa-plus me-2"></i>Create User
          </button>
        )}
      </div>

      {message && (
        <div className={`alert alert-${message.includes('success') ? 'success' : 'danger'} alert-dismissible`}>
          {message}
          <button type="button" className="btn-close" onClick={() => setMessage('')}></button>
        </div>
      )}

      {showCreateForm && (
        <div className="card card-glass p-4 mb-4">
          <h6>Create New User</h6>
          <div className="row">
            <div className="col-md-3">
              <input
                type="text"
                className="form-control"
                placeholder="Username *"
                value={newUser.username}
                onChange={(e) => setNewUser({ ...newUser, username: e.target.value })}
              />
            </div>
            <div className="col-md-3">
              <input
                type="password"
                className="form-control"
                placeholder="Password *"
                value={newUser.password}
                onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
              />
            </div>
            <div className="col-md-2">
              <input
                type="email"
                className="form-control"
                placeholder="Email"
                value={newUser.email}
                onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
              />
            </div>
            <div className="col-md-2">
              <input
                type="text"
                className="form-control"
                placeholder="Phone"
                value={newUser.phone}
                onChange={(e) => setNewUser({ ...newUser, phone: e.target.value })}
              />
            </div>
            <div className="col-md-2">
              <select
                className="form-select"
                value={newUser.role}
                onChange={(e) => setNewUser({ ...newUser, role: e.target.value })}
              >
                {roleOptions.map(role => (
                  <option key={role} value={role}>{role}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="mt-2">
            <button className="btn btn-success" onClick={handleCreateUser}>
              Create User
            </button>
            <button className="btn btn-secondary ms-2" onClick={() => setShowCreateForm(false)}>
              Cancel
            </button>
          </div>
        </div>
      )}

      <div className="card card-glass p-4">
        {loading ? (
          <div className="text-center py-4">
            <div className="spinner-border text-primary"></div>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-hover">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.username}>
                    <td><strong>{u.username}</strong></td>
                    <td>{u.email || 'N/A'}</td>
                    <td>{u.phone || 'N/A'}</td>
                    <td>
                      {isSuperAdmin() ? (
                        <select
                          className="form-select form-select-sm"
                          value={u.role || 'AGENT'}
                          onChange={(e) => handleUpdateRole(u.username, e.target.value)}
                          style={{ width: '120px' }}
                        >
                          {roleOptions.map(role => (
                            <option key={role} value={role}>{role}</option>
                          ))}
                        </select>
                      ) : (
                        <span className="badge bg-info">{u.role || 'AGENT'}</span>
                      )}
                    </td>
                    <td>
                      <span className={`badge bg-${u.enabled ? 'success' : 'danger'}`}>
                        {u.enabled ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>
                      <button
                        className={`btn btn-sm ${u.enabled ? 'btn-warning' : 'btn-success'} me-1`}
                        onClick={() => handleToggleUser(u.username, u.enabled)}
                      >
                        <i className={`fas fa-${u.enabled ? 'pause' : 'play'}`}></i>
                      </button>
                      {u.username !== 'superadmin' && (
                        <button
                          className="btn btn-sm btn-danger"
                          onClick={() => handleDeleteUser(u.username)}
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
        )}
      </div>
    </Layout>
  );
};

export default UserManagement;
