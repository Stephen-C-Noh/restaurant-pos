// Where each role lands after login, and when it hits a route it may not use.
export const homeRoute = (role) => ({ SERVER: '/pos', KITCHEN: '/kds' }[role] ?? '/admin')
