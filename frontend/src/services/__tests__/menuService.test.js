import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { fetchActiveMenuItems, fetchCategories } from '../menuService';

vi.mock('axios');

describe('menuService', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('fetchActiveMenuItems', () => {
        it('should call GET /api/menu/items/active and return data', async () => {
            const mockItems = [{ id: '1', name: 'Burger', basePrice: 12.99 }];
            axios.get.mockResolvedValueOnce({ data: mockItems });

            const result = await fetchActiveMenuItems();

            expect(axios.get).toHaveBeenCalledWith('/api/menu/items/active');
            expect(result).toEqual(mockItems);
        });

        it('should propagate errors', async () => {
            axios.get.mockRejectedValueOnce(new Error('Network Error'));

            await expect(fetchActiveMenuItems()).rejects.toThrow('Network Error');
        });
    });

    describe('fetchCategories', () => {
        it('should call GET /api/menu/categories and return data', async () => {
            const mockCategories = [{ id: '1', name: 'Food' }];
            axios.get.mockResolvedValueOnce({ data: mockCategories });

            const result = await fetchCategories();

            expect(axios.get).toHaveBeenCalledWith('/api/menu/categories');
            expect(result).toEqual(mockCategories);
        });
    });
});
