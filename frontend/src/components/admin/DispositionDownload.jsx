import React, { useState } from 'react';
import Layout from '../Layout/Layout';
import api from '../../api';

const DispositionDownload = () => {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const handleDownload = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage('');

    try {
      const response = await api.get('/dispositions/download', {
        params: { startDate, endDate },
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `dispositions_${startDate}_${endDate}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();

      setMessage('Download started!');
    } catch (err) {
      setMessage('Failed to download dispositions. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <h4 className="mb-4">Download Disposition Report</h4>

      <div className="card card-glass p-4">
        {message && (
          <div className={`alert ${message.includes('started') ? 'alert-success' : 'alert-danger'}`}>
            {message}
          </div>
        )}

        <form onSubmit={handleDownload}>
          <div className="row">
            <div className="col-md-5">
              <div className="mb-3">
                <label className="form-label">Start Date</label>
                <input
                  type="date"
                  className="form-control"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  required
                />
              </div>
            </div>
            <div className="col-md-5">
              <div className="mb-3">
                <label className="form-label">End Date</label>
                <input
                  type="date"
                  className="form-control"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  required
                />
              </div>
            </div>
            <div className="col-md-2 d-flex align-items-end">
              <button type="submit" className="btn btn-success w-100" disabled={loading}>
                {loading ? 'Generating...' : <><i className="fas fa-download me-2"></i>Download</>}
              </button>
            </div>
          </div>
        </form>

        <div className="mt-4">
          <h6>CSV Format Preview:</h6>
          <pre className="bg-light p-3 rounded" style={{ fontSize: '12px' }}>
            {`Lead ID,Name,Phone,Disposition,Notes,Date,Allocated To
L001,John Doe,555-0123,Converted,Customer purchased,2024-01-15,Agent001
L002,Jane Smith,555-0456,Not Interested,No response,2024-01-16,Agent002`}
          </pre>
        </div>
      </div>
    </Layout>
  );
};

export default DispositionDownload;