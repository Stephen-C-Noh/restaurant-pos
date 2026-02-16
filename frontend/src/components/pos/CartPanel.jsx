import OrderTypeSelector from './OrderTypeSelector';
import useOrderStore from '../../stores/useOrderStore';

export default function CartPanel() {
    const {
        cartItems,
        orderType,
        submitting,
        error,
        setOrderType,
        updateQuantity,
        updateSpecialInstructions,
        removeItem,
        submitOrder,
        clearError,
        getSubtotal,
        getTaxAmount,
        getTotal,
        getItemCount,
    } = useOrderStore();

    const itemCount = getItemCount();
    const subtotal = getSubtotal();
    const tax = getTaxAmount();
    const total = getTotal();

    return (
        <div className="w-96 bg-white border-l shadow-lg p-4 flex flex-col h-full">
            <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Current Order</h2>
                {itemCount > 0 && (
                    <span className="bg-primary-600 text-white text-xs font-bold px-2 py-1 rounded-full">
                        {itemCount}
                    </span>
                )}
            </div>

            <OrderTypeSelector selected={orderType} onSelect={setOrderType} />

            {error && (
                <div
                    role="alert"
                    className="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded mb-3 flex justify-between items-start text-sm"
                >
                    <span>{error}</span>
                    <button
                        onClick={clearError}
                        className="text-red-500 hover:text-red-700 ml-2 font-bold"
                        aria-label="Dismiss error"
                    >
                        &times;
                    </button>
                </div>
            )}

            <div className="flex-1 overflow-y-auto mb-4">
                {cartItems.length === 0 ? (
                    <p className="text-gray-400 text-center py-8">
                        Tap a menu item to start an order
                    </p>
                ) : (
                    <div className="space-y-3">
                        {cartItems.map((item) => (
                            <div
                                key={item.menuItem.id}
                                className="border rounded-lg p-3"
                            >
                                <div className="flex justify-between items-start mb-2">
                                    <div className="flex-1">
                                        <h4 className="font-semibold text-gray-900 text-sm">
                                            {item.menuItem.name}
                                        </h4>
                                        <p className="text-gray-500 text-xs">
                                            ${item.menuItem.basePrice.toFixed(2)} each
                                        </p>
                                    </div>
                                    <button
                                        onClick={() => removeItem(item.menuItem.id)}
                                        className="text-gray-400 hover:text-red-500 text-lg leading-none"
                                        aria-label={`Remove ${item.menuItem.name}`}
                                    >
                                        &times;
                                    </button>
                                </div>

                                <div className="flex justify-between items-center">
                                    <div className="flex items-center gap-2">
                                        <button
                                            onClick={() =>
                                                updateQuantity(
                                                    item.menuItem.id,
                                                    item.quantity - 1
                                                )
                                            }
                                            className="w-7 h-7 rounded bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-sm"
                                            aria-label={`Decrease ${item.menuItem.name} quantity`}
                                        >
                                            -
                                        </button>
                                        <span className="text-sm font-semibold w-6 text-center text-gray-900">
                                            {item.quantity}
                                        </span>
                                        <button
                                            onClick={() =>
                                                updateQuantity(
                                                    item.menuItem.id,
                                                    item.quantity + 1
                                                )
                                            }
                                            className="w-7 h-7 rounded bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-sm"
                                            aria-label={`Increase ${item.menuItem.name} quantity`}
                                        >
                                            +
                                        </button>
                                    </div>
                                    <span className="font-semibold text-gray-900 text-sm">
                                        ${(item.menuItem.basePrice * item.quantity).toFixed(2)}
                                    </span>
                                </div>

                                <input
                                    type="text"
                                    placeholder="Special instructions..."
                                    value={item.specialInstructions}
                                    onChange={(e) =>
                                        updateSpecialInstructions(
                                            item.menuItem.id,
                                            e.target.value
                                        )
                                    }
                                    className="w-full mt-2 text-xs border rounded px-2 py-1 text-gray-700 placeholder-gray-400"
                                />
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <div className="border-t pt-4">
                <div className="flex justify-between mb-1 text-sm text-gray-600">
                    <span>Subtotal:</span>
                    <span>${subtotal.toFixed(2)}</span>
                </div>
                <div className="flex justify-between mb-1 text-sm text-gray-600">
                    <span>Tax (5%):</span>
                    <span>${tax.toFixed(2)}</span>
                </div>
                <div className="flex justify-between font-bold text-lg text-gray-900">
                    <span>Total:</span>
                    <span>${total.toFixed(2)}</span>
                </div>
            </div>

            <button
                onClick={submitOrder}
                disabled={cartItems.length === 0 || submitting}
                className="btn-primary w-full mt-4 disabled:opacity-50 disabled:cursor-not-allowed"
                aria-label="Send order to kitchen"
            >
                {submitting ? 'Submitting...' : 'Send to Kitchen'}
            </button>
        </div>
    );
}
