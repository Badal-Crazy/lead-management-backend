import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { API_URL } from '../config';

const LeadProfile = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [lead, setLead] = useState(null);
  const [dispositions, setDispositions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('personal');

  useEffect(() => {
    if (id) {
      fetchLeadDetails();
      fetchDispositions();
    } else {
      setError('No lead ID provided');
      setLoading(false);
    }
  }, [id]);

  const fetchLeadDetails = async () => {
    try {
      console.log('Fetching lead details for ID:', id);
      const response = await axios.get(`${API_URL}/leads/${id}`);
      console.log('Lead details response:', response.data);
      setLead(response.data);
    } catch (err) {
      console.error('Failed to load lead details:', err);
      setError(err.response?.data?.error || 'Failed to load lead details');
    }
  };

  const fetchDispositions = async () => {
    try {
      console.log('Fetching dispositions for lead ID:', id);
      const response = await axios.get(`${API_URL}/dispositions/lead/${id}`);
      console.log('Dispositions response:', response.data);
      setDispositions(response.data || []);
    } catch (err) {
      console.error('Failed to fetch dispositions:', err);
      setDispositions([]);
    } finally {
      setLoading(false);
    }
  };

  // ... rest of the component code remains the same ...
  // (All the render methods remain unchanged)

  // Note: The rest of the component code is the same as before
  // Just make sure all API calls use API_URL from config
};

export default LeadProfile;
