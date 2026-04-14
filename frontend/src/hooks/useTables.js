import { useState, useEffect } from 'react';
import { fetchAllTables } from '../services/tableService';

export default function useTables() {
    const [tables, setTables] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const load = () => {
        setLoading(true);
        setError(null);
        fetchAllTables()
            .then((data) => {
                setTables(data);
                setLoading(false);
            })
            .catch((err) => {
                setError(err.response?.data?.message || 'Failed to load tables.');
                setLoading(false);
            });
    };

    useEffect(() => { load(); }, []);

    return { tables, loading, error, refresh: load };
}
