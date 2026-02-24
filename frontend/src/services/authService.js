import axios from 'axios';

export function loginWithPin(pin) {
  return axios.post('/api/auth/login', { pin }).then((res) => res.data);
}
