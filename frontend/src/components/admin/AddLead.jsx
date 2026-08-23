import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../Layout/Layout';
import uploadService from '../../services/uploadService';
import { leadApi } from '../../api';

const AddLead = () => {
  const navigate = useNavigate();
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('');
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatus, setUploadStatus] = useState('');
  const [uploadComplete, setUploadComplete] = useState(false);
  const [activeUploads, setActiveUploads] = useState([]);
  const [uploadHistory, setUploadHistory] = useState([]);
  const [currentUploadId, setCurrentUploadId] = useState(null);
  const [deletingId, setDeletingId] = useState(null);
  const listenerId = useRef(null);
  
  const [allocationMonthYear, setAllocationMonthYear] = useState('');
  const [lenderProcessName, setLenderProcessName] = useState('');
  const [customUploadName, setCustomUploadName] = useState('');

  const sampleColumns = [
    'DND', 'Phone Number', 'Name', 'User ID', 'AgreementNumber',
    'Amount to be pitched(OS)', 'Case Type', 'Loan ID', 'loan_amount',
    'OS', 'Late Payment Fee', 'Settlement', 'overdue_interest',
    'Waiver', 'overdue_principal', 'initial_interest',
    'DisbursementDate', 'InitialDueDate', 'DPD_bucket', 'DPD',
    'Payment Link(Waiver)', 'Payment Link Settlement',
    'current_city', 'current_state', 'ProductName',
    'last_paid_date', 'last_paid_sum', 'Alt Phone Number'
  ];

  useEffect(() => {
    listenerId.current = uploadService.addListener((uploads) => {
      setActiveUploads(uploads);
      
      if (currentUploadId) {
        const upload = uploads.find(u => u.id === currentUploadId);
        if (upload) {
          setUploadProgress(upload.progress || 0);
          if (upload.status === 'COMPLETED') {
            setUploadComplete(true);
            setUploadStatus('✅ Upload Complete!');
            setLoading(false);
            showMessage('Upload completed successfully!', 'success');
            setCurrentUploadId(null);
            addToHistory(upload);
            setFiles(prev => prev.filter(f => f.id !== upload.id));
          } else if (upload.status === 'FAILED') {
            setUploadStatus('❌ Upload Failed');
            setLoading(false);
            showMessage('Upload failed. Please try again.', 'danger');
            setCurrentUploadId(null);
          } else if (upload.status === 'CANCELLED') {
            setUploadStatus('Upload Cancelled');
            setLoading(false);
            setCurrentUploadId(null);
          }
        }
      }
    });

    loadHistory();

    return () => {
      if (listenerId.current) {
        uploadService.removeListener(listenerId.current);
      }
    };
  }, [currentUploadId]);

  const loadHistory = () => {
    const savedHistory = localStorage.getItem('uploadHistory');
    if (savedHistory) {
      try {
        const parsed = JSON.parse(savedHistory);
        setUploadHistory(parsed);
      } catch (e) {
        setUploadHistory([]);
      }
    } else {
      setUploadHistory([]);
    }
  };

  const generateUploadName = () => {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
    const month = allocationMonthYear || 'unknown';
    const landerName = customUploadName.trim() || 'lander';
    return `${landerName}_${timestamp}_${month}`;
  };

  const addToHistory = (upload) => {
    const mappingData = upload.mappingData || {};
    const newEntry = {
      id: upload.id,
      fileName: upload.fileName,
      originalFileName: upload.originalFileName || upload.fileName,
      status: 'Completed',
      progress: 100,
      uploadedAt: new Date().toISOString(),
      totalRows: upload.result?.count || 0,
      successRows: upload.result?.count || 0,
      errorRows: upload.result?.errors?.length || 0,
      fileSize: (upload.fileSize / 1024 / 1024).toFixed(2) + ' MB',
      allocationMonthYear: mappingData.allocationMonthYear || 'N/A',
      lenderProcessName: mappingData.lenderProcessName || 'N/A',
      uploadedFileName: upload.fileName,
      uploadName: mappingData.uploadName || generateUploadName()
    };
    
    setUploadHistory(prev => {
      const updated = [newEntry, ...prev];
      localStorage.setItem('uploadHistory', JSON.stringify(updated));
      return updated;
    });
  };

  const handleFileChange = (e) => {
    const selectedFiles = Array.from(e.target.files);
    const validFiles = [];
    
    selectedFiles.forEach(file => {
      if (file.size > 50 * 1024 * 1024) {
        showMessage(`File ${file.name} exceeds 50MB limit`, 'danger');
        return;
      }
      if (!file.name.endsWith('.csv')) {
        showMessage(`File ${file.name} is not a CSV`, 'danger');
        return;
      }
      validFiles.push({
        id: Date.now() + '_' + Math.random().toString(36).substring(2, 6),
        file: file,
        name: file.name,
        size: file.size
      });
    });

    if (validFiles.length > 0) {
      setFiles(prev => [...prev, ...validFiles]);
      showMessage(`${validFiles.length} file(s) added successfully`, 'success');
    }
    e.target.value = '';
  };

  const showMessage = (text, type = 'success') => {
    setMessage(text);
    setMessageType(type);
    setTimeout(() => {
      setMessage('');
      setMessageType('');
    }, 10000);
  };

  const handleUploadFile = async (fileItem) => {
    if (!allocationMonthYear) {
      showMessage('Please select Allocation Month Year', 'danger');
      return;
    }

    if (!lenderProcessName) {
      showMessage('Please enter Lender/Process Name', 'danger');
      return;
    }

    if (!customUploadName.trim()) {
      showMessage('Please enter a Lander Name for the upload', 'danger');
      return;
    }

    setLoading(true);
    setMessage('');
    setUploadProgress(0);
    setUploadStatus('Starting upload...');
    setUploadComplete(false);

    const uploadName = generateUploadName();
    console.log('📤 Generated uploadName:', uploadName);
    
    const mappingData = {
      allocationMonthYear: allocationMonthYear,
      lenderProcessName: lenderProcessName,
      uploadName: uploadName
    };

    const fileName = `${uploadName}.csv`;

    try {
      const uploadId = uploadService.startUpload(
        fileItem.file,
        (progress) => {
          setUploadProgress(progress);
          setUploadStatus(`Uploading ${fileItem.name}... ${progress}%`);
          setCurrentUploadId(uploadId);
        },
        (result) => {
          setUploadComplete(true);
          setUploadStatus('✅ Upload Complete!');
          setLoading(false);
          showMessage(`✅ ${fileItem.name} uploaded successfully! ${result.count || 0} leads added.`, 'success');
          setCurrentUploadId(null);
        },
        (error) => {
          setUploadStatus('❌ Upload Failed');
          setLoading(false);
          showMessage(`Failed to upload ${fileItem.name}: ${error.message || 'Please try again'}`, 'danger');
          setCurrentUploadId(null);
        },
        mappingData,
        fileName
      );
      
      setCurrentUploadId(uploadId);

    } catch (err) {
      console.error('Error starting upload:', err);
      showMessage('Failed to start upload. Please try again.', 'danger');
      setLoading(false);
    }
  };

  const handleUploadAll = async () => {
    if (files.length === 0) {
      showMessage('No files to upload', 'warning');
      return;
    }

    if (!allocationMonthYear) {
      showMessage('Please select Allocation Month Year', 'danger');
      return;
    }

    if (!lenderProcessName) {
      showMessage('Please enter Lender/Process Name', 'danger');
      return;
    }

    if (!customUploadName.trim()) {
      showMessage('Please enter a Lander Name for the upload', 'danger');
      return;
    }

    for (const fileItem of files) {
      await handleUploadFile(fileItem);
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
  };

  const handleRemoveFile = (fileId) => {
    setFiles(prev => prev.filter(f => f.id !== fileId));
    showMessage('File removed from queue', 'info');
  };

  const handleClearAllFiles = () => {
    if (files.length === 0) return;
    if (window.confirm('Remove all files from queue?')) {
      setFiles([]);
      showMessage('All files removed', 'info');
    }
  };

  const handleCancelUpload = () => {
    if (currentUploadId) {
      uploadService.cancelUpload(currentUploadId);
      setUploadStatus('Cancelling...');
      showMessage('Upload cancelled', 'warning');
      setLoading(false);
      setCurrentUploadId(null);
      setUploadProgress(0);
    }
  };

  const handleDeleteUpload = async (uploadId, uploadName) => {
    if (!window.confirm(`Delete all leads from "${uploadName}"? This action cannot be undone.`)) {
      return;
    }

    setDeletingId(uploadId);
    try {
      console.log('🗑️ Deleting upload with name:', uploadName);
      const response = await leadApi.deleteLeadsByUploadName(encodeURIComponent(uploadName));
      
      console.log('Delete response:', response.data);
      
      if (response.data && response.data.count > 0) {
        setUploadHistory(prev => {
          const updated = prev.filter(item => item.id !== uploadId);
          localStorage.setItem('uploadHistory', JSON.stringify(updated));
          return updated;
        });

        showMessage(`✅ Deleted ${response.data.count} leads from "${uploadName}"`, 'success');
      } else {
        showMessage('No leads found for this upload', 'warning');
      }
    } catch (err) {
      console.error('Error deleting upload:', err);
      showMessage('Failed to delete upload. Please try again.', 'danger');
    } finally {
      setDeletingId(null);
    }
  };

  const handleClearAllHistory = () => {
    if (!window.confirm('Clear all upload history?')) return;
    setUploadHistory([]);
    localStorage.removeItem('uploadHistory');
    showMessage('All history cleared', 'success');
  };

  const handleDownloadSample = () => {
    const header = sampleColumns.join(',');
    const csvContent = header;

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'sample_leads.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  };

  const getStatusBadge = (status) => {
    if (status === 'Completed' || status === 'COMPLETED') {
      return <span className="badge bg-success"><i className="fas fa-check-circle me-1"></i>Completed</span>;
    } else if (status === 'UPLOADING' || status === 'PROCESSING') {
      return <span className="badge bg-warning text-dark"><i className="fas fa-spinner fa-spin me-1"></i>Processing</span>;
    } else if (status === 'FAILED') {
      return <span className="badge bg-danger"><i className="fas fa-exclamation-circle me-1"></i>Failed</span>;
    } else if (status === 'CANCELLED') {
      return <span className="badge bg-secondary"><i className="fas fa-ban me-1"></i>Cancelled</span>;
    } else {
      return <span className="badge bg-secondary">Pending</span>;
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateString;
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1024 / 1024).toFixed(2) + ' MB';
  };

  const hasActiveUpload = activeUploads.some(u => u.status === 'UPLOADING' || u.status === 'PROCESSING');

  return (
    <Layout>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 className="mb-1">Upload Master Sheet</h4>
          <p className="text-muted">Bulk upload multiple CSV files</p>
        </div>
        <button className="btn btn-outline-secondary" onClick={() => navigate('/admin')}>
          <i className="fas fa-arrow-left me-2"></i>Back to Admin
        </button>
      </div>

      {message && (
        <div className={`alert alert-${messageType} alert-dismissible fade show`} role="alert">
          <i className={`fas fa-${messageType === 'success' ? 'check-circle' : messageType === 'danger' ? 'exclamation-circle' : 'info-circle'} me-2`}></i>
          {message}
          <button type="button" className="btn-close" onClick={() => setMessage('')}></button>
        </div>
      )}

      <div className="row">
        <div className="col-lg-8">
          <div className="card card-glass p-4">
            <div className="text-center mb-4">
              <i className="fas fa-file-csv" style={{ fontSize: '48px', color: '#4A6CF7' }}></i>
              <h5 className="mt-3">Upload Master Sheet</h5>
              <p className="text-muted small">Upload multiple CSV files with lead data</p>
            </div>

            <div className="row mb-4">
              <div className="col-md-4">
                <label className="form-label fw-bold">Lander Name *</label>
                <input
                  type="text"
                  className="form-control form-control-lg"
                  placeholder="Enter lander name (e.g., lander1)"
                  value={customUploadName}
                  onChange={(e) => setCustomUploadName(e.target.value)}
                  required
                  disabled={loading || hasActiveUpload}
                />
                <small className="text-muted">Name for this upload batch</small>
              </div>
              <div className="col-md-4">
                <label className="form-label fw-bold">Allocation Month Year *</label>
                <input
                  type="month"
                  className="form-control form-control-lg"
                  value={allocationMonthYear}
                  onChange={(e) => setAllocationMonthYear(e.target.value)}
                  required
                  disabled={loading || hasActiveUpload}
                />
                <small className="text-muted">Select the allocation month for all leads</small>
              </div>
              <div className="col-md-4">
                <label className="form-label fw-bold">Lender / Process Name *</label>
                <input
                  type="text"
                  className="form-control form-control-lg"
                  placeholder="Enter lender or process name"
                  value={lenderProcessName}
                  onChange={(e) => setLenderProcessName(e.target.value)}
                  required
                  disabled={loading || hasActiveUpload}
                />
                <small className="text-muted">This will be applied to all leads in this upload</small>
              </div>
            </div>

            <div className="mb-4">
              <label className="form-label">Select CSV Files</label>
              <input
                type="file"
                className="form-control form-control-lg"
                accept=".csv"
                multiple
                onChange={handleFileChange}
                disabled={loading || hasActiveUpload}
              />
              <small className="text-muted d-block mt-2">
                Select multiple CSV files at once. Max file size: 50MB each.
                Required columns: <strong>Phone Number</strong>, <strong>Name</strong>
              </small>
            </div>

            {files.length > 0 && (
              <div className="mb-4">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <h6 className="mb-0">Files to Upload ({files.length})</h6>
                  <div>
                    <button 
                      className="btn btn-sm btn-success me-2"
                      onClick={handleUploadAll}
                      disabled={loading || hasActiveUpload}
                    >
                      <i className="fas fa-upload me-1"></i> Upload All
                    </button>
                    <button 
                      className="btn btn-sm btn-danger"
                      onClick={handleClearAllFiles}
                      disabled={loading || hasActiveUpload}
                    >
                      <i className="fas fa-trash me-1"></i> Clear All
                    </button>
                  </div>
                </div>
                <div className="file-queue">
                  {files.map((fileItem) => (
                    <div key={fileItem.id} className="file-item d-flex justify-content-between align-items-center p-2 mb-2" style={{
                      background: 'rgba(255,255,255,0.03)',
                      borderRadius: '8px',
                      border: '1px solid rgba(255,255,255,0.06)'
                    }}>
                      <div>
                        <i className="fas fa-file-csv text-primary me-2"></i>
                        <span>{fileItem.name}</span>
                        <small className="text-muted ms-2">({formatFileSize(fileItem.size)})</small>
                      </div>
                      <div>
                        <button 
                          className="btn btn-sm btn-outline-primary me-1"
                          onClick={() => handleUploadFile(fileItem)}
                          disabled={loading || hasActiveUpload}
                        >
                          <i className="fas fa-upload"></i>
                        </button>
                        <button 
                          className="btn btn-sm btn-danger"
                          onClick={() => handleRemoveFile(fileItem.id)}
                          disabled={loading || hasActiveUpload}
                        >
                          <i className="fas fa-times"></i>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {(uploadProgress > 0 || loading) && (
              <div className="mb-4">
                <div className="d-flex justify-content-between align-items-center mb-1">
                  <small className="text-muted">{uploadStatus || 'Uploading...'}</small>
                  <small className="text-muted fw-bold">{uploadProgress}%</small>
                </div>
                <div className="progress" style={{ height: '28px', borderRadius: '14px', overflow: 'hidden' }}>
                  <div 
                    className={`progress-bar ${uploadProgress < 100 ? 'progress-bar-striped progress-bar-animated' : ''}`}
                    role="progressbar" 
                    style={{ 
                      width: `${uploadProgress}%`,
                      background: uploadProgress === 100 
                        ? 'linear-gradient(90deg, #2D9B7A 0%, #3A8A8A 100%)' 
                        : 'linear-gradient(90deg, #4A6CF7 0%, #5C7CFA 100%)',
                      transition: 'width 0.5s ease',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '13px',
                      fontWeight: '600',
                      color: 'white'
                    }}
                  >
                    {uploadProgress === 100 ? '✅ 100% Complete!' : `${uploadProgress}%`}
                  </div>
                </div>
                {uploadProgress === 100 ? (
                  <div className="mt-2 text-center">
                    <span className="badge bg-success" style={{ fontSize: '14px', padding: '6px 16px' }}>
                      <i className="fas fa-check-circle me-1"></i> Upload Complete
                    </span>
                  </div>
                ) : (
                  <div className="mt-2 text-center">
                    <button 
                      className="btn btn-sm btn-danger"
                      onClick={handleCancelUpload}
                      disabled={uploadProgress === 100}
                    >
                      <i className="fas fa-times me-1"></i> Cancel Upload
                    </button>
                    <small className="text-muted d-block mt-1">Upload continues even if you leave this page</small>
                  </div>
                )}
              </div>
            )}

            <hr className="my-4" />
            
            <div>
              <h6 className="mb-2">CSV Format Preview</h6>
              <div className="bg-light p-3 rounded" style={{ fontSize: '12px', overflow: 'auto' }}>
                <code>
                  {sampleColumns.join(', ')}
                </code>
              </div>
              <button 
                className="btn btn-sm btn-outline-primary mt-2"
                onClick={handleDownloadSample}
              >
                <i className="fas fa-download me-2"></i>Download Sample CSV
              </button>
            </div>
          </div>
        </div>

        <div className="col-lg-4">
          <div className="card card-glass p-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h6 className="mb-0">Upload History</h6>
              <div className="d-flex gap-2">
                {uploadHistory.length > 0 && (
                  <button 
                    className="btn btn-sm btn-outline-danger"
                    onClick={handleClearAllHistory}
                  >
                    <i className="fas fa-trash-alt me-1"></i> Clear All
                  </button>
                )}
                <span className="badge bg-primary">{uploadHistory.length} records</span>
              </div>
            </div>
            
            {uploadHistory.length > 0 ? (
              <div className="upload-history-list" style={{ maxHeight: '500px', overflowY: 'auto' }}>
                {uploadHistory.map((upload) => (
                  <div key={upload.id} className="upload-item p-3 mb-2" style={{
                    background: 'rgba(255,255,255,0.03)',
                    borderRadius: '10px',
                    border: '1px solid rgba(255,255,255,0.06)'
                  }}>
                    <div className="d-flex justify-content-between align-items-start">
                      <div className="flex-grow-1">
                        <div className="d-flex align-items-center gap-2 mb-1 flex-wrap">
                          <i className="fas fa-file-csv text-primary"></i>
                          <strong style={{ fontSize: '14px' }}>{upload.uploadName || upload.fileName}</strong>
                          {getStatusBadge(upload.status)}
                        </div>
                        
                        <div className="d-flex flex-wrap gap-3 mb-2" style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                          <span><i className="fas fa-calendar me-1"></i>{formatDate(upload.uploadedAt)}</span>
                          {upload.fileSize && <span><i className="fas fa-database me-1"></i>{upload.fileSize}</span>}
                          {upload.totalRows > 0 && (
                            <span><i className="fas fa-list me-1"></i>{upload.totalRows} rows</span>
                          )}
                          {upload.successRows !== undefined && upload.successRows > 0 && (
                            <span className="text-success">
                              <i className="fas fa-check-circle me-1"></i>{upload.successRows} success
                            </span>
                          )}
                          {upload.errorRows > 0 && (
                            <span className="text-danger">
                              <i className="fas fa-exclamation-circle me-1"></i>{upload.errorRows} errors
                            </span>
                          )}
                        </div>
                        
                        {(upload.allocationMonthYear || upload.lenderProcessName) && (
                          <div className="d-flex flex-wrap gap-2" style={{ fontSize: '11px' }}>
                            {upload.allocationMonthYear && upload.allocationMonthYear !== 'N/A' && (
                              <span className="badge bg-primary">
                                <i className="fas fa-calendar me-1"></i>{upload.allocationMonthYear}
                              </span>
                            )}
                            {upload.lenderProcessName && upload.lenderProcessName !== 'N/A' && (
                              <span className="badge bg-info">
                                <i className="fas fa-building me-1"></i>{upload.lenderProcessName}
                              </span>
                            )}
                          </div>
                        )}
                      </div>
                      
                      <div className="d-flex gap-1">
                        <button 
                          className="btn btn-sm btn-danger"
                          onClick={() => handleDeleteUpload(upload.id, upload.uploadName || upload.fileName)}
                          disabled={deletingId === upload.id}
                          title="Delete all leads from this upload"
                        >
                          {deletingId === upload.id ? (
                            <span className="spinner-border spinner-border-sm"></span>
                          ) : (
                            <i className="fas fa-trash"></i>
                          )}
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-4">
                <i className="fas fa-history" style={{ fontSize: '36px', color: 'var(--text-muted)' }}></i>
                <p className="text-muted mt-3">No upload history yet</p>
                <p className="text-muted small">Uploads will appear here after completion</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default AddLead;
