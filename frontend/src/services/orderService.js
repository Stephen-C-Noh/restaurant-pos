import api from './api';

export function createOrder(request) {
    return api.post('/api/orders', request).then(res => res.data);
}

export function fireOrder(orderId) {
    return api.post(`/api/orders/${orderId}/fire`).then(res => res.data);
}

export function fetchActiveOrders() {
    return api.get('/api/orders/active').then(res => res.data);
}
