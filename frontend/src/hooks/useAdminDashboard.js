import { useState, useEffect } from 'react';
import { fetchDashboardStats } from '../services/adminService';

export default function useAdminDashboard() {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const load = () => {
        setLoading(true);
        setError(null);
        fetchDashboardStats()
            .then((data) => {
                setStats(data);
                setLoading(false);
            })
            .catch((err) => {
                setError(err.response?.data?.message || 'Failed to load dashboard.');
                setLoading(false);
            });
    };

    useEffect(() => { load(); }, []);

    return { stats, loading, error, retry: load };
}
