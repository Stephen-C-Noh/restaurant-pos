import api from './api';

export const fetchAllTables = () => api.get('/api/tables').then(r => r.data);
export const fetchAvailableTables = () => api.get('/api/tables/available').then(r => r.data);
export const updateTableStatus = (id, status, serverId = null) =>
    api.patch(`/api/tables/${id}/status`, { status, ...(serverId && { serverId }) }).then(r => r.data);
export const createTable = (tableNumber, capacity, zone) =>
    api.post('/api/tables', { tableNumber, capacity, zone }).then(r => r.data);
export const deleteTable = (id) => api.delete(`/api/tables/${id}`);
