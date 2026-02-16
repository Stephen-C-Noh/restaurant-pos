import axios from 'axios';

export function fetchActiveMenuItems() {
    return axios.get('/api/menu/items/active').then(res => res.data);
}

export function fetchCategories() {
    return axios.get('/api/menu/categories').then(res => res.data);
}
