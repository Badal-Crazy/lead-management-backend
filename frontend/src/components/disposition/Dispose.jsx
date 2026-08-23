import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../Layout/Layout';
import api from '../../api';

const Dispose = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [lead, setLead] = useState(null);
  const [loading, setLoading] = useState(false);
  const [searching, setSearching] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('');
  const [formData, setFormData] = useState({
    leadId: '',
    agreementNumber: '',
    name: '',
    phone: '',
    disposition: '',
    callDate: '',
    callTime: '',
    amount: '',
    paymentDate: '',
    paymentAmount: '',
    remarks: ''
  });

  const dispositionOptions = [
    'PTP', 'Paid', 'Part Paid', 'CB', 'RTP', 'RNR', 'LM'
  ];

  // Dispositions that need additional fields
  const needsCallFields = ['PTP', 'CB'];
  const needsPaymentFields = ['Paid', 'Part Paid'];

  // Fetch lead by ID if coming from profile
  useEffect(() => {
    if (id) {
      fetchLeadById(id);
    }
  }, [id]);

  // Search lead by Agreement Number
  const searchLeadByAgreement = async (agreementNumber) => {
    if (!agreementNumber || agreementNumber.trim().length < 3) {
      return;
    }

    setSearching(true);
    try {
      const response = await api.get(`/leads/search?q=${agreementNumber.trim()}&type=agreement`);
      const results = response.data?.data || [];
      
      if (results.length > 0) {
        const foundLead = results[0];
        setLead(foundLead);
        setFormData(prev => ({
          ...prev,
          leadId: foundLead.id || '',
          agreementNumber: foundLead.agreementNumber || '',
          name: foundLead.name || '',
          phone: foundLead.phoneNumber || ''
        }));
        showMessage('Lead found! Details auto-filled.', 'success');
      } else {
        setFormData(prev => ({
          ...prev,
          leadId: '',
          name: '',
          phone: ''
        }));
        showMessage('No lead found with this Agreement Number', 'warning');
      }
    } catch (err) {
      console.error('Error searching lead:', err);
      showMessage('Error searching for lead', 'danger');
    } finally {
      setSearching(false);
    }
  };

  const fetchLeadById = async (leadId) => {
    setLoading(true);
    try {
      const response = await api.get(`/leads/${leadId}`);
      const leadData = response.data;
      setLead(leadData);
      
      setFormData({
        leadId: leadData.id || '',
        agreementNumber: leadData.agreementNumber || '',
        name: leadData.name || '',
        phone: leadData.phoneNumber || '',
        disposition: '',
        callDate: '',
        callTime: '',
        amount: '',
        paymentDate: '',
        paymentAmount: '',
        remarks: ''
      });
    } catch (err) {
      showMessage('Failed to fetch lead details', 'danger');
      console.error('Error fetching lead:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleAgreementChange = (e) => {
    const value = e.target.value;
    setFormData(prev => ({
      ...prev,
      agreementNumber: value,
      name: '',
      phone: ''
    }));
  };

  const handleAgreementBlur = (e) => {
    const value = e.target.value.trim();
    if (value && !id) {
      searchLeadByAgreement(value);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      const value = e.target.value.trim();
      if (value && !id) {
        searchLeadByAgreement(value);
      }
    }
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const showMessage = (text, type = 'success') => {
    setMessage(text);
    setMessageType(type);
    setTimeout(() => {
      setMessage('');
      setMessageType('');
    }, 3000);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validate
    if (!formData.leadId || !formData.name) {
      showMessage('Please enter a valid Agreement Number to find the lead', 'danger');
      return;
    }

    if (!formData.disposition) {
      showMessage('Please select a disposition', 'danger');
      return;
    }

    setSubmitting(true);
    setMessage('');

    try {
      const payload = {
        leadId: parseInt(formData.leadId),
        leadName: formData.name,
        leadPhone: formData.phone,
        dispositionStatus: formData.disposition,
        callDate: formData.callDate || null,
        callTime: formData.callTime || null,
        amount: formData.amount ? parseFloat(formData.amount) : null,
        paymentDate: formData.paymentDate || null,
        paymentAmount: formData.paymentAmount ? parseFloat(formData.paymentAmount) : null,
        notes: formData.remarks,
        disposedBy: JSON.parse(localStorage.getItem('user'))?.username || 'Unknown'
      };

      await api.post('/dispositions', payload);
      
      // Show success message immediately
      showMessage('✅ Disposition saved successfully!', 'success');
      
      // Reset form after 1 second
      setTimeout(() => {
        if (!id) {
          setFormData({
            leadId: '',
            agreementNumber: '',
            name: '',
            phone: '',
            disposition: '',
            callDate: '',
            callTime: '',
            amount: '',
            paymentDate: '',
            paymentAmount: '',
            remarks: ''
          });
        } else {
          setFormData(prev => ({
            ...prev,
            disposition: '',
            callDate: '',
            callTime: '',
            amount: '',
            paymentDate: '',
            paymentAmount: '',
            remarks: ''
          }));
        }
        setSubmitting(false);
        // Navigate back to profile after 1.5 seconds
        setTimeout(() => {
          if (id) {
            navigate(`/lead/${id}`);
          }
        }, 1500);
      }, 1000);
      
    } catch (err) {
      showMessage(err.response?.data?.error || 'Failed to save disposition', 'danger');
      setSubmitting(false);
    }
  };

  const renderAdditionalFields = () => {
    const disposition = formData.disposition;
    
    // For RTP, RNR, LM - no additional fields
    if (['RTP', 'RNR', 'LM'].includes(disposition)) {
      return null;
    }

    // For PTP and CB - show call fields
    if (['PTP', 'CB'].includes(disposition)) {
      return (
        <>
          <div className="row">
            <div className="col-md-4 mb-3">
              <label className="form-label">Call Date</label>
              <input
                type="date"
                name="callDate"
                className="form-control"
                value={formData.callDate}
                onChange={handleChange}
              />
            </div>
            <div className="col-md-4 mb-3">
              <label className="form-label">Call Time</label>
              <input
                type="time"
                name="callTime"
                className="form-control"
                value={formData.callTime}
                onChange={handleChange}
              />
            </div>
            <div className="col-md-4 mb-3">
              <label className="form-label">Amount (Optional)</label>
              <input
                type="number"
                name="amount"
                className="form-control"
                placeholder="Enter amount if applicable"
                value={formData.amount}
                onChange={handleChange}
              />
            </div>
          </div>
        </>
      );
    }

    // For Paid and Part Paid - show payment fields
    if (['Paid', 'Part Paid'].includes(disposition)) {
      return (
        <>
          <div className="row">
            <div className="col-md-6 mb-3">
              <label className="form-label">Payment Date</label>
              <input
                type="date"
                name="paymentDate"
                className="form-control"
                value={formData.paymentDate}
                onChange={handleChange}
              />
            </div>
            <div className="col-md-6 mb-3">
              <label className="form-label">Payment Amount</label>
              <input
                type="number"
                name="paymentAmount"
                className="form-control"
                placeholder="Enter payment amount"
                value={formData.paymentAmount}
                onChange={handleChange}
              />
            </div>
          </div>
        </>
      );
    }

    return null;
  };

  return (
    <Layout>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 className="mb-1">Lead Disposition</h4>
          <p className="text-muted">Enter Agreement Number to auto-fill details</p>
        </div>
        <button className="btn btn-outline-secondary" onClick={() => navigate('/search')}>
          <i className="fas fa-arrow-left me-2"></i>Back to Search
        </button>
      </div>

      {message && (
        <div className={`alert alert-${messageType} alert-dismissible fade show`}>
          <i className={`fas fa-${messageType === 'success' ? 'check-circle' : messageType === 'danger' ? 'exclamation-circle' : 'info-circle'} me-2`}></i>
          {message}
          <button type="button" className="btn-close" onClick={() => setMessage('')}></button>
        </div>
      )}

      <div className="row justify-content-center">
        <div className="col-lg-8">
          <div className="card card-glass p-4">
            <form onSubmit={handleSubmit}>
              {/* Agreement Number */}
              <div className="mb-3">
                <label className="form-label">Agreement Number *</label>
                <div className="input-group">
                  <input
                    type="text"
                    name="agreementNumber"
                    className="form-control form-control-lg"
                    value={formData.agreementNumber}
                    onChange={handleAgreementChange}
                    onBlur={handleAgreementBlur}
                    onKeyDown={handleKeyDown}
                    placeholder="Enter Agreement Number to auto-fill"
                    required
                    readOnly={!!id}
                    style={{
                      fontWeight: formData.name ? '600' : '400',
                      color: formData.name ? '#1a1a2e' : '#6c757d'
                    }}
                  />
                  {!id && (
                    <button 
                      type="button" 
                      className="btn btn-outline-primary"
                      onClick={() => searchLeadByAgreement(formData.agreementNumber)}
                      disabled={searching || !formData.agreementNumber.trim()}
                    >
                      {searching ? (
                        <span className="spinner-border spinner-border-sm"></span>
                      ) : (
                        <i className="fas fa-search"></i>
                      )}
                    </button>
                  )}
                </div>
                {!id && (
                  <small className="text-muted">
                    <i className="fas fa-info-circle me-1"></i>
                    Enter Agreement Number and press Enter or click Search
                  </small>
                )}
                {id && (
                  <small className="text-muted">
                    <i className="fas fa-check-circle text-success me-1"></i>
                    Auto-filled from profile
                  </small>
                )}
              </div>

              {/* Name - Auto-filled */}
              <div className="mb-3">
                <label className="form-label">Name *</label>
                <input
                  type="text"
                  name="name"
                  className="form-control form-control-lg"
                  value={formData.name}
                  readOnly
                  style={{ 
                    background: formData.name ? '#f5f7fa' : '#ffffff',
                    fontWeight: '500',
                    color: formData.name ? '#1a1a2e' : '#6c757d'
                  }}
                  required
                  placeholder={formData.name ? '' : 'Auto-fills from Agreement Number'}
                />
              </div>

              {/* Phone - Auto-filled */}
              <div className="mb-3">
                <label className="form-label">Phone</label>
                <input
                  type="text"
                  name="phone"
                  className="form-control form-control-lg"
                  value={formData.phone}
                  readOnly
                  style={{ 
                    background: formData.phone ? '#f5f7fa' : '#ffffff',
                    color: formData.phone ? '#1a1a2e' : '#6c757d'
                  }}
                  placeholder={formData.phone ? '' : 'Auto-fills from Agreement Number'}
                />
              </div>

              {/* Disposition */}
              <div className="mb-3">
                <label className="form-label">Disposition *</label>
                <select
                  name="disposition"
                  className="form-select form-select-lg"
                  value={formData.disposition}
                  onChange={handleChange}
                  required
                  disabled={!formData.name || submitting}
                >
                  <option value="">Select disposition...</option>
                  {dispositionOptions.map(option => (
                    <option key={option} value={option}>{option}</option>
                  ))}
                </select>
                {!formData.name && (
                  <small className="text-muted text-danger">
                    Please enter a valid Agreement Number first
                  </small>
                )}
                {formData.name && (
                  <small className="text-muted">
                    <i className="fas fa-info-circle me-1"></i>
                    {formData.disposition && ['PTP', 'CB'].includes(formData.disposition) && 'Call Date, Time, and Amount fields will appear'}
                    {formData.disposition && ['Paid', 'Part Paid'].includes(formData.disposition) && 'Payment Date and Amount fields will appear'}
                    {formData.disposition && ['RTP', 'RNR', 'LM'].includes(formData.disposition) && 'No additional fields needed'}
                  </small>
                )}
              </div>

              {/* Additional fields based on disposition */}
              {renderAdditionalFields()}

              {/* Remarks */}
              <div className="mb-4">
                <label className="form-label">Remarks</label>
                <textarea
                  name="remarks"
                  className="form-control"
                  rows="4"
                  placeholder="Add remarks about this disposition..."
                  value={formData.remarks}
                  onChange={handleChange}
                  style={{ fontSize: '15px' }}
                  disabled={submitting}
                />
              </div>

              <div className="d-flex gap-2">
                <button 
                  type="submit" 
                  className="btn btn-primary btn-lg" 
                  disabled={submitting || !formData.name}
                  style={{ minWidth: '150px' }}
                >
                  {submitting ? (
                    <><span className="spinner-border spinner-border-sm me-2"></span>Saving...</>
                  ) : (
                    <><i className="fas fa-save me-2"></i>Save Disposition</>
                  )}
                </button>
                <button 
                  type="reset" 
                  className="btn btn-secondary btn-lg"
                  onClick={() => {
                    if (!id) {
                      setFormData({
                        leadId: '',
                        agreementNumber: '',
                        name: '',
                        phone: '',
                        disposition: '',
                        callDate: '',
                        callTime: '',
                        amount: '',
                        paymentDate: '',
                        paymentAmount: '',
                        remarks: ''
                      });
                    } else {
                      setFormData({
                        ...formData,
                        disposition: '',
                        callDate: '',
                        callTime: '',
                        amount: '',
                        paymentDate: '',
                        paymentAmount: '',
                        remarks: ''
                      });
                    }
                  }}
                  disabled={submitting}
                >
                  Clear
                </button>
                {id && (
                  <button 
                    type="button" 
                    className="btn btn-outline-info btn-lg"
                    onClick={() => navigate(`/lead/${id}`)}
                    disabled={submitting}
                  >
                    <i className="fas fa-arrow-left me-2"></i>Back to Profile
                  </button>
                )}
              </div>
            </form>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default Dispose;
