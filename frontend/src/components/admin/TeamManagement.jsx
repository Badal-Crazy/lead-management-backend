import React, { useEffect, useState } from 'react';
import Layout from '../Layout/Layout';
import { teamApi } from '../../api';
import { useAuth } from '../../context/AuthContext';

const TeamManagement = () => {
  const { isSuperAdmin } = useAuth();
  const [teams, setTeams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newTeam, setNewTeam] = useState({ name: '', description: '' });

  useEffect(() => {
    fetchTeams();
  }, []);

  const fetchTeams = async () => {
    try {
      const response = await teamApi.getTeams();
      setTeams(response.data || []);
    } catch (err) {
      console.error('Failed to fetch teams:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateTeam = async () => {
    if (!newTeam.name.trim()) {
      setMessage('Team name is required');
      return;
    }
    try {
      await teamApi.createTeam(newTeam);
      setMessage('Team created successfully');
      setShowCreateForm(false);
      setNewTeam({ name: '', description: '' });
      fetchTeams();
    } catch (err) {
      setMessage('Failed to create team');
    }
  };

  const handleDeleteTeam = async (teamId) => {
    if (!window.confirm('Delete this team?')) return;
    try {
      await teamApi.deleteTeam(teamId);
      setMessage('Team deleted successfully');
      fetchTeams();
    } catch (err) {
      setMessage('Failed to delete team');
    }
  };

  return (
    <Layout>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 className="mb-1">Team Management</h4>
          <p className="text-muted">Manage teams and team members</p>
        </div>
        {isSuperAdmin() && (
          <button className="btn btn-primary" onClick={() => setShowCreateForm(!showCreateForm)}>
            <i className="fas fa-plus me-2"></i>Create Team
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
          <h6>Create New Team</h6>
          <div className="row">
            <div className="col-md-4">
              <input
                type="text"
                className="form-control"
                placeholder="Team Name *"
                value={newTeam.name}
                onChange={(e) => setNewTeam({ ...newTeam, name: e.target.value })}
              />
            </div>
            <div className="col-md-4">
              <input
                type="text"
                className="form-control"
                placeholder="Description"
                value={newTeam.description}
                onChange={(e) => setNewTeam({ ...newTeam, description: e.target.value })}
              />
            </div>
            <div className="col-md-4">
              <button className="btn btn-success" onClick={handleCreateTeam}>
                Create Team
              </button>
              <button className="btn btn-secondary ms-2" onClick={() => setShowCreateForm(false)}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="row">
        {loading ? (
          <div className="text-center py-4">
            <div className="spinner-border text-primary"></div>
          </div>
        ) : teams.length > 0 ? (
          teams.map((team) => (
            <div className="col-md-4 mb-4" key={team.id}>
              <div className="card card-glass p-4">
                <div className="d-flex justify-content-between align-items-center">
                  <h6 className="mb-0">{team.name}</h6>
                  {isSuperAdmin() && (
                    <button 
                      className="btn btn-sm btn-danger"
                      onClick={() => handleDeleteTeam(team.id)}
                    >
                      <i className="fas fa-trash"></i>
                    </button>
                  )}
                </div>
                <p className="text-muted small mt-2">{team.description || 'No description'}</p>
                <hr />
                <small className="text-muted">
                  Members: {team.userIds?.length || 0}
                </small>
                <div className="mt-2">
                  {team.userIds?.slice(0, 3).map((userId) => (
                    <span key={userId} className="badge bg-secondary me-1">
                      {userId}
                    </span>
                  ))}
                  {team.userIds?.length > 3 && (
                    <span className="badge bg-secondary">+{team.userIds.length - 3} more</span>
                  )}
                </div>
              </div>
            </div>
          ))
        ) : (
          <div className="col-12">
            <div className="text-center py-4">
              <i className="fas fa-users" style={{ fontSize: '48px', color: '#ccc' }}></i>
              <p className="text-muted mt-3">No teams created yet</p>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
};

export default TeamManagement;
