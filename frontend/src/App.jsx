import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import AgentDashboard from './components/dashboard/AgentDashboard';
import Dashboard from './components/dashboard/Dashboard';
import Search from './components/search/Search';
import Dispose from './components/disposition/Dispose';
import LeadProfile from './components/LeadProfile';
import Profile from './components/profile/Profile';
import AdminDashboard from './components/admin/AdminDashboard';
import SuperAdminDashboard from './components/superadmin/SuperAdminDashboard';
import AddLead from './components/admin/AddLead';
import AgentApproval from './components/admin/AgentApproval';
import DispositionDownload from './components/admin/DispositionDownload';
import Reports from './components/reports/Reports';
import TeamManagement from './components/admin/TeamManagement';
import UserManagement from './components/admin/UserManagement';
import PrivateRoute from './components/common/PrivateRoute';
import './styles/global.css';
import 'bootstrap/dist/css/bootstrap.min.css';
import '@fortawesome/fontawesome-free/css/all.min.css';

function AppRoutes() {
  const { user, isAdmin, isSuperAdmin } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      
      {/* Agent Routes */}
      <Route path="/dashboard" element={
        <PrivateRoute>
          {isAdmin() || isSuperAdmin() ? <AdminDashboard /> : <AgentDashboard />}
        </PrivateRoute>
      } />
      <Route path="/search" element={<PrivateRoute><Search /></PrivateRoute>} />
      <Route path="/dispose" element={<PrivateRoute><Dispose /></PrivateRoute>} />
      <Route path="/dispose/:id" element={<PrivateRoute><Dispose /></PrivateRoute>} />
      <Route path="/lead/:id" element={<PrivateRoute><LeadProfile /></PrivateRoute>} />
      <Route path="/profile" element={<PrivateRoute><Profile /></PrivateRoute>} />
      
      {/* Admin Routes */}
      <Route path="/admin" element={
        <PrivateRoute adminOnly>
          {isSuperAdmin() ? <SuperAdminDashboard /> : <AdminDashboard />}
        </PrivateRoute>
      } />
      <Route path="/admin/add-lead" element={<PrivateRoute adminOnly><AddLead /></PrivateRoute>} />
      <Route path="/admin/approvals" element={<PrivateRoute adminOnly><AgentApproval /></PrivateRoute>} />
      <Route path="/admin/disposition-download" element={<PrivateRoute adminOnly><DispositionDownload /></PrivateRoute>} />
      <Route path="/admin/users" element={<PrivateRoute superAdminOnly><UserManagement /></PrivateRoute>} />
      <Route path="/admin/teams" element={<PrivateRoute adminOnly><TeamManagement /></PrivateRoute>} />
      <Route path="/reports" element={<PrivateRoute><Reports /></PrivateRoute>} />
      
      <Route path="/" element={<Navigate to="/dashboard" />} />
    </Routes>
  );
}

function App() {
  return (
    <Router future={{
      v7_startTransition: true,
      v7_relativeSplatPath: true
    }}>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </Router>
  );
}

export default App;
