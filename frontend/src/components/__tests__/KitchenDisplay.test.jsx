import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, waitFor } from '@testing-library/react';

vi.mock('../../services/api', () => ({
    default: { get: vi.fn(), patch: vi.fn() },
}));
vi.mock('sockjs-client', () => ({ default: vi.fn() }));

// Capture the config KitchenDisplay hands to the STOMP client so the test can fire onConnect itself.
let stompConfig;
vi.mock('@stomp/stompjs', () => ({
    Client: vi.fn(function (config) {
        stompConfig = config;
        this.subscribe = vi.fn();
        this.activate = vi.fn();
        this.deactivate = vi.fn();
    }),
}));

const { default: api } = await import('../../services/api');
const { default: KitchenDisplay } = await import('../KitchenDisplay');

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
});
