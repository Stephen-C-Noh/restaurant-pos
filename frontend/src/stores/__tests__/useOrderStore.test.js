import { describe, it, expect, vi, beforeEach } from 'vitest';
import { act } from '@testing-library/react';

// Mock the service before importing the store
vi.mock('../../services/orderService', () => ({
    createOrder: vi.fn(),
    fireOrder: vi.fn(),
}));

const { default: useOrderStore } = await import('../useOrderStore');
const { createOrder, fireOrder } = await import('../../services/orderService');

const mockMenuItem = {
    id: 'item-1',
    name: 'Classic Burger',
    basePrice: 12.99,
    kitchenSection: 'GRILL',
};

const mockMenuItem2 = {
    id: 'item-2',
    name: 'Caesar Salad',
    basePrice: 9.99,
    kitchenSection: 'COLD',
};

describe('useOrderStore', () => {
    beforeEach(() => {
        act(() => {
            useOrderStore.getState().clearCart();
            useOrderStore.getState().resetOrder();
            useOrderStore.getState().clearError();
        });
        vi.clearAllMocks();
    });

    describe('addItem', () => {
        it('should add a new item to cart', () => {
            act(() => useOrderStore.getState().addItem(mockMenuItem));

            const { cartItems } = useOrderStore.getState();
            expect(cartItems).toHaveLength(1);
            expect(cartItems[0].menuItem.id).toBe('item-1');
            expect(cartItems[0].quantity).toBe(1);
            expect(cartItems[0].specialInstructions).toBe('');
        });

        it('should increment quantity for existing item', () => {
            act(() => {
                useOrderStore.getState().addItem(mockMenuItem);
                useOrderStore.getState().addItem(mockMenuItem);
            });

            const { cartItems } = useOrderStore.getState();
            expect(cartItems).toHaveLength(1);
            expect(cartItems[0].quantity).toBe(2);
        });
    });

    describe('removeItem', () => {
        it('should remove item from cart', () => {
            act(() => {
                useOrderStore.getState().addItem(mockMenuItem);
                useOrderStore.getState().removeItem('item-1');
            });

            expect(useOrderStore.getState().cartItems).toHaveLength(0);
        });
    });

    describe('updateQuantity', () => {
        it('should update quantity', () => {
            act(() => {
                useOrderStore.getState().addItem(mockMenuItem);
                useOrderStore.getState().updateQuantity('item-1', 5);
            });

            expect(useOrderStore.getState().cartItems[0].quantity).toBe(5);
        });

        it('should remove item when quantity is 0', () => {
            act(() => {
                useOrderStore.getState().addItem(mockMenuItem);
                useOrderStore.getState().updateQuantity('item-1', 0);
            });

            expect(useOrderStore.getState().cartItems).toHaveLength(0);
        });

        it('should cap quantity at 99', () => {
            act(() => {
                useOrderStore.getState().addItem(mockMenuItem);
                useOrderStore.getState().updateQuantity('item-1', 150);
            });

            expect(useOrderStore.getState().cartItems[0].quantity).toBe(99);
        });
    });

    describe('updateSpecialInstructions', () => {
        it('should set special instructions', () => {
            act(() => {
                useOrderStore.getState().addItem(mockMenuItem);
                useOrderStore.getState().updateSpecialInstructions('item-1', 'No onions');
            });

            expect(useOrderStore.getState().cartItems[0].specialInstructions).toBe('No onions');
        });
    });

    describe('setOrderType', () => {
        it('should change order type', () => {
            act(() => useOrderStore.getState().setOrderType('TAKEOUT'));

            expect(useOrderStore.getState().orderType).toBe('TAKEOUT');
        });
    });

    describe('computed values', () => {
        it('should calculate subtotal, tax, and total', () => {
            act(() => {
                useOrderStore.getState().addItem(mockMenuItem);  // 12.99
                useOrderStore.getState().addItem(mockMenuItem2); // 9.99
            });

            const state = useOrderStore.getState();
            expect(state.getSubtotal()).toBeCloseTo(22.98, 2);
            expect(state.getTaxAmount()).toBeCloseTo(1.149, 2);
            expect(state.getTotal()).toBeCloseTo(24.129, 2);
            expect(state.getItemCount()).toBe(2);
        });
    });

    describe('submitOrder', () => {
        it('should create and fire order on success', async () => {
            const mockOrder = { id: 'order-1', orderNumber: 'ORD-001', status: 'DRAFT' };
            const mockFired = { id: 'order-1', orderNumber: 'ORD-001', status: 'FIRED' };
            createOrder.mockResolvedValueOnce(mockOrder);
            fireOrder.mockResolvedValueOnce(mockFired);

            act(() => useOrderStore.getState().addItem(mockMenuItem));

            await act(async () => {
                await useOrderStore.getState().submitOrder();
            });

            const state = useOrderStore.getState();
            expect(state.lastOrder).toEqual(mockFired);
            expect(state.cartItems).toHaveLength(0);
            expect(state.submitting).toBe(false);
            expect(state.error).toBeNull();
            expect(createOrder).toHaveBeenCalledWith({
                orderType: 'DINE_IN',
                tableId: null,
                notes: null,
                items: [{ menuItemId: 'item-1', quantity: 1, specialInstructions: null }],
            });
            expect(fireOrder).toHaveBeenCalledWith('order-1');
        });

        it('should set error and keep cart on create failure', async () => {
            const error = new Error('fail');
            error.response = { data: { message: 'Server error' } };
            createOrder.mockRejectedValueOnce(error);

            act(() => useOrderStore.getState().addItem(mockMenuItem));

            await act(async () => {
                await useOrderStore.getState().submitOrder();
            });

            const state = useOrderStore.getState();
            expect(state.error).toBe('Server error');
            expect(state.cartItems).toHaveLength(1);
            expect(state.submitting).toBe(false);
            expect(state.lastOrder).toBeNull();
        });

        it('should handle fire failure after create succeeds', async () => {
            const mockOrder = { id: 'order-1', orderNumber: 'ORD-001', status: 'DRAFT' };
            createOrder.mockResolvedValueOnce(mockOrder);
            fireOrder.mockRejectedValueOnce(new Error('fire failed'));

            act(() => useOrderStore.getState().addItem(mockMenuItem));

            await act(async () => {
                await useOrderStore.getState().submitOrder();
            });

            const state = useOrderStore.getState();
            expect(state.lastOrder).toEqual(mockOrder);
            expect(state.cartItems).toHaveLength(0);
            expect(state.error).toContain('ORD-001');
            expect(state.error).toContain('could not be sent to kitchen');
        });
    });

    describe('clearCart', () => {
        it('should reset cart state', () => {
            act(() => {
                useOrderStore.getState().addItem(mockMenuItem);
                useOrderStore.getState().setOrderType('TAKEOUT');
                useOrderStore.getState().clearCart();
            });

            const state = useOrderStore.getState();
            expect(state.cartItems).toHaveLength(0);
            expect(state.orderType).toBe('DINE_IN');
            expect(state.orderNotes).toBe('');
        });
    });

    describe('resetOrder', () => {
        it('should clear lastOrder', () => {
            act(() => {
                useOrderStore.setState({ lastOrder: { id: 'test' } });
                useOrderStore.getState().resetOrder();
            });

            expect(useOrderStore.getState().lastOrder).toBeNull();
        });
    });
});
