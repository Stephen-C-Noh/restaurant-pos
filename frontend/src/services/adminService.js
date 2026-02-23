import axios from 'axios';

export function fetchDashboardStats() {
    return axios.get('/api/admin/stats').then(res => res.data);
}
