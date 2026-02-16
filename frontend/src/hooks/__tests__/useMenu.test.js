import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';

vi.mock('../../services/menuService', () => ({
    fetchActiveMenuItems: vi.fn(),
    fetchCategories: vi.fn(),
}));

const { fetchActiveMenuItems, fetchCategories } = await import('../../services/menuService');
const { default: useMenu } = await import('../useMenu');

const mockItems = [
    { id: '1', name: 'Burger', kitchenSection: 'GRILL', basePrice: 12.99 },
    { id: '2', name: 'Salad', kitchenSection: 'COLD', basePrice: 9.99 },
    { id: '3', name: 'Fries', kitchenSection: 'FRYER', basePrice: 5.99 },
    { id: '4', name: 'Steak', kitchenSection: 'GRILL', basePrice: 24.99 },
];

const mockCategories = [{ id: 'cat-1', name: 'Food & Beverages' }];

describe('useMenu', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should fetch menu items and categories on mount', async () => {
        fetchActiveMenuItems.mockResolvedValueOnce(mockItems);
        fetchCategories.mockResolvedValueOnce(mockCategories);

        const { result } = renderHook(() => useMenu());

        expect(result.current.loading).toBe(true);

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.menuItems).toEqual(mockItems);
        expect(result.current.categories).toEqual(mockCategories);
        expect(result.current.error).toBeNull();
    });

    it('should group items by section', async () => {
        fetchActiveMenuItems.mockResolvedValueOnce(mockItems);
        fetchCategories.mockResolvedValueOnce(mockCategories);

        const { result } = renderHook(() => useMenu());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.menuItemsBySection.GRILL).toHaveLength(2);
        expect(result.current.menuItemsBySection.COLD).toHaveLength(1);
        expect(result.current.menuItemsBySection.FRYER).toHaveLength(1);
    });

    it('should extract unique sections', async () => {
        fetchActiveMenuItems.mockResolvedValueOnce(mockItems);
        fetchCategories.mockResolvedValueOnce(mockCategories);

        const { result } = renderHook(() => useMenu());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.sections).toContain('GRILL');
        expect(result.current.sections).toContain('COLD');
        expect(result.current.sections).toContain('FRYER');
        expect(result.current.sections).toHaveLength(3);
    });

    it('should filter by selected section', async () => {
        fetchActiveMenuItems.mockResolvedValueOnce(mockItems);
        fetchCategories.mockResolvedValueOnce(mockCategories);

        const { result } = renderHook(() => useMenu());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.filteredItems).toHaveLength(4); // ALL by default

        act(() => {
            result.current.setSelectedSection('GRILL');
        });

        expect(result.current.filteredItems).toHaveLength(2);
        expect(result.current.filteredItems[0].name).toBe('Burger');
    });

    it('should handle API errors', async () => {
        const error = new Error('Network Error');
        error.response = { data: { message: 'Service unavailable' } };
        fetchActiveMenuItems.mockRejectedValueOnce(error);
        fetchCategories.mockResolvedValueOnce(mockCategories);

        const { result } = renderHook(() => useMenu());

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.error).toBe('Service unavailable');
        expect(result.current.menuItems).toHaveLength(0);
    });
});
