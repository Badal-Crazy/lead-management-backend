import React, { createContext, useState, useContext, useEffect } from 'react';
import { authApi } from '../api';

const AuthContext = createContext(null);

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser);
        setUser(parsedUser);
        setIsAuthenticated(true);
      } catch (e) {
        localStorage.removeItem('user');
      }
    }
    setLoading(false);
  }, []);

  const login = async (username, password) => {
    try {
      const response = await authApi.login({ username, password });
      if (response.data) {
        const userData = {
          ...response.data,
          isAuthenticated: true
        };
        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
        setIsAuthenticated(true);
        return { success: true, data: userData };
      }
      return { success: false, error: 'Login failed' };
    } catch (error) {
      return { 
        success: false, 
        error: error.response?.data?.error || 'Login failed. Please try again.' 
      };
    }
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch (error) {
      console.debug('Logout error:', error);
    } finally {
      localStorage.removeItem('user');
      setUser(null);
      setIsAuthenticated(false);
    }
  };

  const forceLogout = async (username) => {
    try {
      await authApi.forceLogout(username);
      if (user?.username === username) {
        localStorage.removeItem('user');
        setUser(null);
        setIsAuthenticated(false);
      }
      return { success: true };
    } catch (error) {
      return { success: false, error: error.response?.data?.error };
    }
  };

  const isSuperAdmin = () => {
    const role = user?.role || '';
    return role === 'SUPER_ADMIN' || role === 'ROLE_SUPER_ADMIN';
  };

  const isAdmin = () => {
    const role = user?.role || '';
    return role === 'ADMIN' || role === 'ROLE_ADMIN' || role === 'SUPER_ADMIN' || role === 'ROLE_SUPER_ADMIN';
  };

  const isAgent = () => {
    const role = user?.role || '';
    return role === 'AGENT' || role === 'ROLE_AGENT';
  };

  const value = {
    user,
    setUser,
    loading,
    isAuthenticated,
    login,
    logout,
    forceLogout,
    isAdmin,
    isSuperAdmin,
    isAgent,
  };

  return React.createElement(AuthContext.Provider, { value }, children);
}

export default AuthContext;
