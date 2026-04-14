import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../api';
import { createOrder, fireOrder, fetchActiveOrders } from '../orderService';

vi.mock('../api', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
    },
}));

describe('orderService', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('createOrder', () => {
        it('should POST to /api/orders with request body and return data', async () => {
            const request = {
                orderType: 'DINE_IN',
                items: [{ menuItemId: 'abc', quantity: 2 }],
            };
            const mockResponse = { id: 'order-1', orderNumber: 'ORD-001', status: 'DRAFT' };
            api.post.mockResolvedValueOnce({ data: mockResponse });

            const result = await createOrder(request);

            expect(api.post).toHaveBeenCalledWith('/api/orders', request);
            expect(result).toEqual(mockResponse);
        });

        it('should propagate API errors', async () => {
            const error = new Error('Bad Request');
            error.response = { status: 400, data: { message: 'Invalid order' } };
            api.post.mockRejectedValueOnce(error);

            await expect(createOrder({})).rejects.toThrow('Bad Request');
        });
    });

    describe('fireOrder', () => {
        it('should POST to /api/orders/{id}/fire and return data', async () => {
            const mockResponse = { id: 'order-1', status: 'FIRED' };
            api.post.mockResolvedValueOnce({ data: mockResponse });

            const result = await fireOrder('order-1');

            expect(api.post).toHaveBeenCalledWith('/api/orders/order-1/fire');
            expect(result).toEqual(mockResponse);
        });
    });

    describe('fetchActiveOrders', () => {
        it('should GET /api/orders/active and return data', async () => {
            const mockOrders = [{ id: '1', status: 'FIRED' }];
            api.get.mockResolvedValueOnce({ data: mockOrders });

            const result = await fetchActiveOrders();

            expect(api.get).toHaveBeenCalledWith('/api/orders/active');
            expect(result).toEqual(mockOrders);
        });
    });
});
