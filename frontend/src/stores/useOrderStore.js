import { create } from 'zustand';
import { createOrder, fireOrder } from '../services/orderService';

const TAX_RATE = 0.05;

const useOrderStore = create((set, get) => ({
    cartItems: [],
    orderType: 'DINE_IN',
    orderNotes: '',
    submitting: false,
    lastOrder: null,
    error: null,

    addItem: (menuItem) => {
        set((state) => {
            const existing = state.cartItems.find(
                (item) => item.menuItem.id === menuItem.id
            );
            if (existing) {
                return {
                    cartItems: state.cartItems.map((item) =>
                        item.menuItem.id === menuItem.id
                            ? { ...item, quantity: item.quantity + 1 }
                            : item
                    ),
                };
            }
            return {
                cartItems: [
                    ...state.cartItems,
                    { menuItem, quantity: 1, specialInstructions: '' },
                ],
            };
        });
    },

    removeItem: (menuItemId) => {
        set((state) => ({
            cartItems: state.cartItems.filter(
                (item) => item.menuItem.id !== menuItemId
            ),
        }));
    },

    updateQuantity: (menuItemId, quantity) => {
        if (quantity <= 0) {
            get().removeItem(menuItemId);
            return;
        }
        const capped = Math.min(quantity, 99);
        set((state) => ({
            cartItems: state.cartItems.map((item) =>
                item.menuItem.id === menuItemId
                    ? { ...item, quantity: capped }
                    : item
            ),
        }));
    },

    updateSpecialInstructions: (menuItemId, instructions) => {
        set((state) => ({
            cartItems: state.cartItems.map((item) =>
                item.menuItem.id === menuItemId
                    ? { ...item, specialInstructions: instructions }
                    : item
            ),
        }));
    },

    setOrderType: (orderType) => set({ orderType }),
    setOrderNotes: (orderNotes) => set({ orderNotes }),
    setError: (error) => set({ error }),
    clearError: () => set({ error: null }),

    getSubtotal: () => {
        const { cartItems } = get();
        return cartItems.reduce(
            (sum, item) => sum + item.menuItem.basePrice * item.quantity,
            0
        );
    },

    getTaxAmount: () => get().getSubtotal() * TAX_RATE,
    getTotal: () => get().getSubtotal() * (1 + TAX_RATE),
    getItemCount: () =>
        get().cartItems.reduce((sum, item) => sum + item.quantity, 0),

    submitOrder: async (tableId) => {
        const { cartItems, orderType, orderNotes } = get();
        set({ submitting: true, error: null });

        try {
            const request = {
                orderType,
                tableId: tableId ?? null,
                notes: orderNotes || null,
                items: cartItems.map((item) => ({
                    menuItemId: item.menuItem.id,
                    quantity: item.quantity,
                    specialInstructions: item.specialInstructions || null,
                })),
            };

            const order = await createOrder(request);

            let firedOrder;
            try {
                firedOrder = await fireOrder(order.id);
            } catch (fireErr) {
                // Order was created but fire failed — show what we have
                set({
                    lastOrder: order,
                    cartItems: [],
                    orderNotes: '',
                    submitting: false,
                    error: `Order ${order.orderNumber} created but could not be sent to kitchen. Please notify a manager.`,
                });
                return;
            }

            set({
                lastOrder: firedOrder,
                cartItems: [],
                orderNotes: '',
                submitting: false,
            });
        } catch (err) {
            const message =
                err.response?.data?.message ||
                err.response?.data ||
                'Failed to submit order. Please try again.';
            set({ error: String(message), submitting: false });
        }
    },

    clearCart: () => set({ cartItems: [], orderNotes: '', orderType: 'DINE_IN', error: null }),

    resetOrder: () => set({ lastOrder: null }),
}));

export default useOrderStore;
