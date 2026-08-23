import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../../api';

const Register = () => {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    phone: '',
    role: 'AGENT'
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [emailError, setEmailError] = useState('');
  const navigate = useNavigate();

  const validateEmail = (email) => {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return emailRegex.test(email);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    
    // Validate email on change
    if (name === 'username' || name === 'email') {
      if (value && !validateEmail(value)) {
        setEmailError('Please enter a valid email address');
      } else {
        setEmailError('');
      }
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    // Validate email
    if (!validateEmail(formData.username)) {
      setError('Please enter a valid email address as username');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }

    setLoading(true);

    try {
      const { confirmPassword, ...userData } = formData;
      const response = await authApi.signup(userData);
      setSuccess(response.data.message || 'Registration successful! Please wait for admin approval.');
      setTimeout(() => navigate('/login'), 3000);
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center" 
         style={{ 
           background: 'linear-gradient(135deg, #0c0c1d 0%, #1a1a3e 50%, #2d1b69 100%)',
         }}>
      <div className="container">
        <div className="row justify-content-center">
          <div className="col-md-6">
            <div className="card border-0 shadow-lg" style={{
              background: 'rgba(255, 255, 255, 0.08)',
              backdropFilter: 'blur(20px)',
              borderRadius: '24px',
              border: '1px solid rgba(255, 255, 255, 0.12)',
            }}>
              <div className="card-body p-5">
                <div className="text-center mb-4">
                  <h2 className="text-white fw-bold">Register</h2>
                  <p className="text-light opacity-75">Create your account</p>
                </div>

                {error && <div className="alert alert-danger">{error}</div>}
                {success && <div className="alert alert-success">{success}</div>}

                <form onSubmit={handleSubmit}>
                  <div className="mb-3">
                    <label className="form-label text-light">Email Address (Username) *</label>
                    <input
                      type="email"
                      name="username"
                      className={`form-control bg-transparent text-light ${emailError ? 'is-invalid' : ''}`}
                      style={{ border: '1px solid rgba(255,255,255,0.1)' }}
                      placeholder="Enter your email address"
                      value={formData.username}
                      onChange={handleChange}
                      required
                    />
                    {emailError && (
                      <div className="invalid-feedback d-block">
                        {emailError}
                      </div>
                    )}
                    <small className="text-light opacity-50">
                      Your email will be used as your username
                    </small>
                  </div>

                  <div className="row">
                    <div className="col-md-6">
                      <div className="mb-3">
                        <label className="form-label text-light">Password *</label>
                        <input
                          type="password"
                          name="password"
                          className="form-control bg-transparent text-light"
                          style={{ border: '1px solid rgba(255,255,255,0.1)' }}
                          placeholder="Min 6 characters"
                          value={formData.password}
                          onChange={handleChange}
                          required
                        />
                      </div>
                    </div>
                    <div className="col-md-6">
                      <div className="mb-3">
                        <label className="form-label text-light">Confirm Password *</label>
                        <input
                          type="password"
                          name="confirmPassword"
                          className="form-control bg-transparent text-light"
                          style={{ border: '1px solid rgba(255,255,255,0.1)' }}
                          placeholder="Confirm your password"
                          value={formData.confirmPassword}
                          onChange={handleChange}
                          required
                        />
                      </div>
                    </div>
                  </div>

                  <div className="mb-3">
                    <label className="form-label text-light">Email</label>
                    <input
                      type="email"
                      name="email"
                      className="form-control bg-transparent text-light"
                      style={{ border: '1px solid rgba(255,255,255,0.1)' }}
                      placeholder="Enter your email"
                      value={formData.email}
                      onChange={handleChange}
                    />
                  </div>

                  <div className="mb-3">
                    <label className="form-label text-light">Phone</label>
                    <input
                      type="tel"
                      name="phone"
                      className="form-control bg-transparent text-light"
                      style={{ border: '1px solid rgba(255,255,255,0.1)' }}
                      placeholder="Enter your phone number"
                      value={formData.phone}
                      onChange={handleChange}
                    />
                  </div>

                  <div className="mb-3">
                    <label className="form-label text-light">Role</label>
                    <select 
                      name="role" 
                      className="form-select bg-transparent text-light"
                      style={{ border: '1px solid rgba(255,255,255,0.1)' }}
                      value={formData.role} 
                      onChange={handleChange}
                    >
                      <option value="AGENT" className="text-dark">Agent</option>
                    </select>
                    <small className="text-light opacity-50">Admin approval required for access</small>
                  </div>

                  <button 
                    type="submit" 
                    className="btn w-100 py-3 fw-bold"
                    disabled={loading || !!emailError}
                    style={{
                      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                      border: 'none',
                      borderRadius: '12px',
                      color: 'white',
                      opacity: loading || emailError ? 0.7 : 1,
                      cursor: loading || emailError ? 'not-allowed' : 'pointer'
                    }}
                  >
                    {loading ? (
                      <><span className="spinner-border spinner-border-sm me-2"></span>Registering...</>
                    ) : (
                      'Register'
                    )}
                  </button>
                </form>

                <div className="text-center mt-3">
                  <p className="text-light opacity-75">
                    Already have an account? <Link to="/login" className="text-primary">Login here</Link>
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;
