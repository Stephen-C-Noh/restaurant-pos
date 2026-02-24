import api from './api';

export function fetchDashboardStats() {
    return api.get('/api/admin/stats').then(res => res.data);
}
