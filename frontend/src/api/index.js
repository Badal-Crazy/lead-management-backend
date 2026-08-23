import axios from 'axios';
import { API_URL } from '../config';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 600000,
});

// Request interceptor - add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// ============ 1. Authentication APIs ============
export const authApi = {
  signup: (data) => api.post('/auth/signup', data),
  login: (data) => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout'),
  forceLogout: (username) => api.post('/auth/force-logout', { username }),
  refreshToken: () => api.post('/auth/refresh-token'),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  resetPassword: (data) => api.post('/auth/reset-password', data),
};

// ============ 2. Profile APIs ============
export const profileApi = {
  getProfile: () => api.get('/profile/me'),
  updateProfile: (data) => api.put('/profile/me', data),
  changePassword: (data) => api.put('/profile/change-password', data),
  uploadAvatar: (file) => {
    const formData = new FormData();
    formData.append('avatar', file);
    return api.post('/profile/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

// ============ 3. User Management APIs ============
export const userApi = {
  getUsers: () => api.get('/users'),
  getUser: (userId) => api.get(`/users/${userId}`),
  updateUser: (userId, data) => api.put(`/users/${userId}`, data),
  approveUser: (userId) => api.post(`/users/${userId}/approve`),
  rejectUser: (userId) => api.post(`/users/${userId}/reject`),
  activateUser: (userId) => api.post(`/users/${userId}/activate`),
  deactivateUser: (userId) => api.post(`/users/${userId}/deactivate`),
  forceLogoutUser: (userId) => api.post(`/users/${userId}/force-logout`),
  getLoginStatus: (userId) => api.get(`/users/${userId}/login-status`),
  getAllUsers: () => api.get('/users/all'),
  createUser: (data) => api.post('/users/create', data),
  deleteUser: (userId) => api.delete(`/users/${userId}`),
  updateUserRole: (userId, role) => api.put(`/users/${userId}/role`, { role }),
  assignAdmin: (userId, adminId) => api.put(`/users/${userId}/admin`, { adminId }),
  assignTeam: (userId, teamId) => api.put(`/users/${userId}/team`, { teamId }),
};

// ============ 4. Role Management APIs ============
export const roleApi = {
  getRoles: () => api.get('/roles'),
  createRole: (data) => api.post('/roles', data),
  updateRole: (roleId, data) => api.put(`/roles/${roleId}`, data),
  deleteRole: (roleId) => api.delete(`/roles/${roleId}`),
};

// ============ 5. Team Management APIs ============
export const teamApi = {
  getTeams: () => api.get('/teams'),
  createTeam: (data) => api.post('/teams', data),
  getTeam: (teamId) => api.get(`/teams/${teamId}`),
  updateTeam: (teamId, data) => api.put(`/teams/${teamId}`, data),
  deleteTeam: (teamId) => api.delete(`/teams/${teamId}`),
  addUserToTeam: (teamId, userId) => api.post(`/teams/${teamId}/users`, { userId }),
  removeUserFromTeam: (teamId, userId) => api.delete(`/teams/${teamId}/users/${userId}`),
  getTeamUsers: (teamId) => api.get(`/teams/${teamId}/users`),
};

// ============ 6. Admin-Agent Mapping APIs ============
export const mappingApi = {
  getMappings: () => api.get('/admin-agent-mapping'),
  createMapping: (data) => api.post('/admin-agent-mapping', data),
  updateMapping: (id, data) => api.put(`/admin-agent-mapping/${id}`, data),
  deleteMapping: (id) => api.delete(`/admin-agent-mapping/${id}`),
  getAdminAgents: (adminId) => api.get(`/admins/${adminId}/agents`),
  getAgentAdmin: (agentId) => api.get(`/agents/${agentId}/admin`),
};

// ============ 7. Lead Management APIs ============
export const leadApi = {
  getMyAllocation: () => api.get('/leads/my-allocation'),
  getMyAllocationSummary: () => api.get('/leads/my-allocation/summary'),
  getLead: (leadId) => api.get(`/leads/${leadId}`),
  searchMyAllocation: (query) => api.get(`/leads/my-allocation/search?q=${query}`),
  getTeamLeads: () => api.get('/leads/team'),
  getUnallocatedLeads: () => api.get('/leads/unallocated'),
  getLeads: () => api.get('/leads'),
  uploadLeads: (file, onProgress) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/leads/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress,
    });
  },
  getUploadProgress: (uploadId) => api.get(`/leads/upload/${uploadId}/progress`),
  getUploadStatus: (uploadId) => api.get(`/leads/upload/${uploadId}/status`),
  bulkAssign: (data) => api.post('/leads/bulk-assign', data),
  reassignLead: (data) => api.post('/leads/reassign', data),
  deleteLead: (leadId) => api.delete(`/leads/${leadId}`),
  deleteLeadsByUploadName: (uploadName) => api.delete(`/leads/upload/${uploadName}`),
  bulkDeleteLeads: (leadIds) => api.delete('/leads/bulk', { data: { leadIds } }),
  getUploadHistory: () => api.get('/leads/upload-history'),
  searchLeads: (query, type) => api.get(`/leads/search?q=${query}&type=${type}`),
  getAllLeads: () => api.get('/leads/all'),
  createLead: (data) => api.post('/leads/create', data),
  updateLead: (leadId, data) => api.put(`/leads/${leadId}`, data),
  bulkUploadLeads: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/leads/bulk-upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  bulkReassignLeads: (data) => api.post('/leads/bulk-reassign', data),
};

// ============ 8. Allocation APIs ============
export const allocationApi = {
  getMyAllocations: () => api.get('/allocations/my'),
  getAllocations: () => api.get('/allocations'),
  createAllocation: (data) => api.post('/allocations', data),
  bulkCreateAllocations: (data) => api.post('/allocations/bulk', data),
  updateAllocation: (allocationId, data) => api.put(`/allocations/${allocationId}`, data),
  reassignAllocation: (data) => api.post('/allocations/reassign', data),
  deleteAllocation: (allocationId) => api.delete(`/allocations/${allocationId}`),
  getAgentAllocations: (agentId) => api.get(`/allocations/agent/${agentId}`),
  getAdminAllocations: (adminId) => api.get(`/allocations/admin/${adminId}`),
  getUnallocated: () => api.get('/allocations/unallocated'),
};

// ============ 9. Disposition APIs ============
export const dispositionApi = {
  createDisposition: (data) => api.post('/dispositions', data),
  getMyDispositions: () => api.get('/dispositions/my'),
  getMyDispositionSummary: () => api.get('/dispositions/my/summary'),
  getMyDateWiseDispositions: (date) => api.get(`/dispositions/my/date-wise?date=${date}`),
  getTeamDispositions: () => api.get('/dispositions/team'),
  getAgentDispositions: (agentId) => api.get(`/dispositions/agent/${agentId}`),
  getDispositionSummary: () => api.get('/dispositions/summary'),
  getDateWiseDispositions: (date) => api.get(`/dispositions/date-wise?date=${date}`),
  getAllDispositions: () => api.get('/dispositions/all'),
  getAllDispositionSummary: () => api.get('/dispositions/summary/all'),
  getAdminDispositions: (adminId) => api.get(`/dispositions/admin/${adminId}`),
  downloadDispositions: (startDate, endDate) => 
    api.get(`/dispositions/download?startDate=${startDate}&endDate=${endDate}`, {
      responseType: 'blob',
    }),
};

// ============ 10. Disposition Master APIs ============
export const dispositionTypeApi = {
  getTypes: () => api.get('/disposition-types'),
  createType: (data) => api.post('/disposition-types', data),
  updateType: (id, data) => api.put(`/disposition-types/${id}`, data),
  deleteType: (id) => api.delete(`/disposition-types/${id}`),
};

// ============ 11. Dashboard APIs ============
export const dashboardApi = {
  getAgentDashboard: () => api.get('/dashboard/agent'),
  getAdminDashboard: () => api.get('/dashboard/admin'),
  getSuperAdminDashboard: () => api.get('/dashboard/super-admin'),
};

// ============ 12. Reports APIs ============
export const reportsApi = {
  getAgentPerformance: (params) => api.get('/reports/agent-performance', { params }),
  getAdminPerformance: (params) => api.get('/reports/admin-performance', { params }),
  getTeamPerformance: (params) => api.get('/reports/team-performance', { params }),
  getLeadStatus: (params) => api.get('/reports/lead-status', { params }),
  getDispositionSummary: (params) => api.get('/reports/disposition-summary', { params }),
  getAllocationSummary: (params) => api.get('/reports/allocation-summary', { params }),
  getLoginLogout: (params) => api.get('/reports/login-logout', { params }),
  getDateWiseReport: (params) => api.get('/reports/date-wise', { params }),
  exportReport: (params) => api.get('/reports/export', { params, responseType: 'blob' }),
};

// ============ 13. Lead Upload APIs ============
export const uploadApi = {
  uploadLeads: (file, onProgress) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/uploads/leads', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress,
    });
  },
  getUploadProgress: (uploadId) => api.get(`/uploads/${uploadId}/progress`),
  getUploadStatus: (uploadId) => api.get(`/uploads/${uploadId}/status`),
  getUploadResult: (uploadId) => api.get(`/uploads/${uploadId}/result`),
  getUploadErrors: (uploadId) => api.get(`/uploads/${uploadId}/errors`),
  cancelUpload: (uploadId) => api.post(`/uploads/${uploadId}/cancel`),
  getUploadHistory: () => api.get('/uploads/history'),
};

// ============ 14. Bulk Operation APIs ============
export const bulkApi = {
  assignLeads: (data) => api.post('/bulk/leads/assign', data),
  reassignLeads: (data) => api.post('/bulk/leads/reassign', data),
  deleteLeads: (data) => api.delete('/bulk/leads/delete', { data }),
  updateLeads: (data) => api.post('/bulk/leads/update', data),
  activateUsers: (data) => api.post('/bulk/users/activate', data),
  deactivateUsers: (data) => api.post('/bulk/users/deactivate', data),
  assignTeamToUsers: (data) => api.post('/bulk/users/assign-team', data),
  assignAdminToUsers: (data) => api.post('/bulk/users/assign-admin', data),
};

// ============ 15. User Activity APIs ============
export const activityApi = {
  getMyActivity: () => api.get('/activity/my'),
  getAgentActivity: (agentId) => api.get(`/activity/agent/${agentId}`),
  getAdminActivity: (adminId) => api.get(`/activity/admin/${adminId}`),
  getAllActivity: () => api.get('/activity/all'),
};

// ============ 16. Notification APIs ============
export const notificationApi = {
  getNotifications: () => api.get('/notifications'),
  createNotification: (data) => api.post('/notifications', data),
  markAsRead: (id) => api.put(`/notifications/${id}/read`),
  markAllAsRead: () => api.put('/notifications/read-all'),
};

// ============ 17. Super Admin System APIs ============
export const systemApi = {
  getSettings: () => api.get('/system/settings'),
  updateSettings: (data) => api.put('/system/settings', data),
  getAuditLogs: () => api.get('/system/audit-logs'),
  getAllSessions: () => api.get('/system/all-sessions'),
  forceLogoutUser: (userId) => api.post(`/system/force-logout/${userId}`),
  getUserActivity: (userId) => api.get(`/system/user-activity?userId=${userId}`),
  getDatabaseSummary: () => api.get('/system/database-summary'),
};

// ============ Admin APIs ============
export const adminApi = {
  getStats: () => api.get('/admin/stats'),
  getRecentActivity: () => api.get('/admin/recent-activity'),
  getPendingUsers: () => api.get('/admin/pending-users'),
  approveUser: (username) => api.post(`/admin/approve-user/${username}`, { action: 'approve' }),
  rejectUser: (username) => api.post(`/admin/approve-user/${username}`, { action: 'reject' }),
  getAllUsers: () => api.get('/admin/users'),
  createUser: (data) => api.post('/admin/users', data),
  updateUserRole: (username, role) => api.put(`/admin/users/${username}/role`, { role }),
  toggleUser: (username, enabled) => api.put(`/admin/users/${username}/toggle`, { enabled }),
  deleteUser: (username) => api.delete(`/admin/users/${username}`),
  changePassword: (username, password) => api.put(`/admin/users/${username}/password`, { password }),
};

export default api;
