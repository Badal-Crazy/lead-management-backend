// ============================================
// MASTER CRM - CENTRALIZED CONFIGURATION
// ============================================

// --- API Configuration ---
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
export const API_URL = `${API_BASE_URL}/api`;

// --- Frontend Configuration ---
export const FRONTEND_URL = import.meta.env.VITE_FRONTEND_URL || 'http://localhost:5173';
export const APP_NAME = 'Master CRM';
export const APP_VERSION = '2.0.0';

// --- Feature Flags ---
export const FEATURES = {
  enableFileUpload: true,
  enableBulkDelete: true,
  enableReports: true,
};

// --- Default Values ---
export const DEFAULTS = {
  pageSize: 10,
  maxFileSize: 50 * 1024 * 1024, // 50MB
  uploadTimeout: 600000, // 10 minutes
};

// --- Export all as default ---
export default {
  API_BASE_URL,
  API_URL,
  FRONTEND_URL,
  APP_NAME,
  APP_VERSION,
  FEATURES,
  DEFAULTS,
};
