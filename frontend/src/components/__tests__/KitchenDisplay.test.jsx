import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, act, waitFor } from '@testing-library/react';

vi.mock('../../services/api', () => ({
    default: { get: vi.fn(), patch: vi.fn() },
}));
vi.mock('sockjs-client', () => ({ default: vi.fn() }));

// Capture the config KitchenDisplay hands to the STOMP client so the test can fire onConnect itself.
let stompConfig;
let pushOrder; // the /topic/orders callback
vi.mock('@stomp/stompjs', () => ({
    Client: vi.fn(function (config) {
        stompConfig = config;
        this.subscribe = vi.fn((destination, callback) => { pushOrder = callback; });
        this.activate = vi.fn();
        this.deactivate = vi.fn();
    }),
}));

const { default: api } = await import('../../services/api');
const { default: KitchenDisplay } = await import('../KitchenDisplay');
const { default: useAuthStore } = await import('../../stores/useAuthStore');

describe('KitchenDisplay', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        api.get.mockResolvedValue({ data: [] });
        window.AudioContext = vi.fn(function () {
            this.state = 'running';
        });
    });

    it('reloads the active orders on every (re)connect', async () => {
        render(<KitchenDisplay />);
        await waitFor(() => expect(api.get).toHaveBeenCalledTimes(1)); // initial load on mount

        stompConfig.onConnect(); // first connect
        stompConfig.onDisconnect();
        stompConfig.onConnect(); // reconnect

        await waitFor(() => expect(api.get).toHaveBeenCalledTimes(3));
        expect(api.get).toHaveBeenLastCalledWith(expect.stringContaining('/api/orders/active'));
    });

    it('removes a ticket when a pushed order is no longer FIRED, PREPARING or READY', async () => {
        const fired = {
            id: 'o1', orderNumber: 'ORD-1', status: 'FIRED', firedAt: new Date().toISOString(),
            items: [{ id: 'i1', menuItemName: 'Burger', quantity: 1, status: 'FIRED', section: 'GRILL' }],
        };
        api.get.mockResolvedValue({ data: [fired] });
        render(<KitchenDisplay />);
        await screen.findByText('ORD-1');
        await act(async () => { stompConfig.onConnect(); }); // settles the reload that every connect triggers

        act(() => pushOrder({ body: JSON.stringify({ ...fired, status: 'CANCELLED' }) }));

        await waitFor(() => expect(screen.queryByText('ORD-1')).toBeNull());
    });

    it('authenticates the STOMP connection with the logged-in token', async () => {
        useAuthStore.setState({ token: 'jwt-123', user: { role: 'KITCHEN' } });
        render(<KitchenDisplay />);
        await waitFor(() => expect(api.get).toHaveBeenCalled());

        expect(stompConfig.connectHeaders).toEqual({ Authorization: 'Bearer jwt-123' });
    });

    it('logs out when the server rejects the STOMP connection (e.g. an expired token), so the route sends it to login', async () => {
        useAuthStore.setState({ token: 'expired', user: { role: 'KITCHEN' } });
        render(<KitchenDisplay />);
        await waitFor(() => expect(api.get).toHaveBeenCalled());

        act(() => stompConfig.onStompError({ headers: { message: 'Invalid token' } }));

        expect(useAuthStore.getState().token).toBeNull();
    });
});
