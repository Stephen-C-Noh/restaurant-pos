import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../api';
import { fetchActiveMenuItems, fetchCategories } from '../menuService';

vi.mock('../api', () => ({
    default: {
        get: vi.fn(),
    },
}));

describe('menuService', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('fetchActiveMenuItems', () => {
        it('should call GET /api/menu/items/active and return data', async () => {
            const mockItems = [{ id: '1', name: 'Burger', basePrice: 12.99 }];
            api.get.mockResolvedValueOnce({ data: mockItems });

            const result = await fetchActiveMenuItems();

            expect(api.get).toHaveBeenCalledWith('/api/menu/items/active');
            expect(result).toEqual(mockItems);
        });

        it('should propagate errors', async () => {
            api.get.mockRejectedValueOnce(new Error('Network Error'));

            await expect(fetchActiveMenuItems()).rejects.toThrow('Network Error');
        });
    });

    describe('fetchCategories', () => {
        it('should call GET /api/menu/categories and return data', async () => {
            const mockCategories = [{ id: '1', name: 'Food' }];
            api.get.mockResolvedValueOnce({ data: mockCategories });

            const result = await fetchCategories();

            expect(api.get).toHaveBeenCalledWith('/api/menu/categories');
            expect(result).toEqual(mockCategories);
        });
    });
});
