import api from './api';

export function fetchActiveMenuItems() {
    return api.get('/api/menu/items/active').then(res => res.data);
}

export function fetchCategories() {
    return api.get('/api/menu/categories').then(res => res.data);
}
