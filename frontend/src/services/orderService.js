import axios from 'axios';

export function createOrder(request) {
    return axios.post('/api/orders', request).then(res => res.data);
}

export function fireOrder(orderId) {
    return axios.post(`/api/orders/${orderId}/fire`).then(res => res.data);
}

export function fetchActiveOrders() {
    return axios.get('/api/orders/active').then(res => res.data);
}
