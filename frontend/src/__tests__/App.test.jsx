import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';

// Each page is a marker: these tests are about which route a user lands on.
vi.mock('../components/Login', () => ({ default: () => <div>login page</div> }));
vi.mock('../components/KitchenDisplay', () => ({ default: () => <div>kitchen page</div> }));
vi.mock('../components/AdminDashboard', () => ({ default: () => <div>admin page</div> }));
vi.mock('../components/POSTerminal', () => ({ default: () => <div>pos page</div> }));
vi.mock('../components/Tables', () => ({ default: () => <div>tables page</div> }));

const { default: App } = await import('../App');
const { default: useAuthStore } = await import('../stores/useAuthStore');

function visit(path, user) {
    useAuthStore.setState(user ? { token: 't', user } : { token: null, user: null });
    window.history.pushState({}, '', path);
    render(<App />);
}

describe('App routes', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('sends /kds without a token to the login page', () => {
        visit('/kds', null);

        expect(screen.getByText('login page')).toBeInTheDocument();
        expect(screen.queryByText('kitchen page')).toBeNull();
    });

    it('sends a kitchen user on a route it may not use to /kds, not in a loop', () => {
        visit('/admin', { id: 'k1', username: 'kitchen1', role: 'KITCHEN' });

        expect(screen.getByText('kitchen page')).toBeInTheDocument();
        expect(window.location.pathname).toBe('/kds');
    });
});
