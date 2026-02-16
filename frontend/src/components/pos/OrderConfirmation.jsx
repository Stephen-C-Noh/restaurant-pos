import { useEffect, useState } from 'react';
import useOrderStore from '../../stores/useOrderStore';

const AUTO_RESET_SECONDS = 5;

export default function OrderConfirmation() {
    const { lastOrder, resetOrder } = useOrderStore();
    const [countdown, setCountdown] = useState(AUTO_RESET_SECONDS);

    useEffect(() => {
        if (!lastOrder) return;

        setCountdown(AUTO_RESET_SECONDS);
        const timer = setInterval(() => {
            setCountdown((prev) => {
                if (prev <= 1) {
                    clearInterval(timer);
                    resetOrder();
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);

        return () => clearInterval(timer);
    }, [lastOrder, resetOrder]);

    if (!lastOrder) return null;

    return (
        <div className="w-96 bg-white border-l shadow-lg p-4 flex flex-col h-full items-center justify-center">
            <div className="text-center">
                <div className="text-green-500 text-5xl mb-4">&#10003;</div>
                <h2 className="text-2xl font-bold text-gray-900 mb-2">Order Sent!</h2>
                <p className="text-3xl font-bold text-primary-600 mb-6">
                    {lastOrder.orderNumber}
                </p>

                <div className="text-left bg-gray-50 rounded-lg p-4 mb-6 w-full">
                    <p className="text-sm text-gray-500 mb-2">
                        {lastOrder.orderType.replace('_', ' ')}
                    </p>
                    <div className="space-y-1 mb-3">
                        {lastOrder.items.map((item) => (
                            <div
                                key={item.id}
                                className="flex justify-between text-sm text-gray-700"
                            >
                                <span>
                                    {item.quantity}x {item.menuItemName}
                                </span>
                                <span>${item.subtotal.toFixed(2)}</span>
                            </div>
                        ))}
                    </div>
                    <div className="border-t pt-2 space-y-1">
                        <div className="flex justify-between text-sm text-gray-600">
                            <span>Subtotal</span>
                            <span>${lastOrder.subtotal.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between text-sm text-gray-600">
                            <span>Tax</span>
                            <span>${lastOrder.taxAmount.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between font-bold text-gray-900">
                            <span>Total</span>
                            <span>${lastOrder.total.toFixed(2)}</span>
                        </div>
                    </div>
                </div>

                <button
                    onClick={resetOrder}
                    className="btn-primary w-full"
                    aria-label="Start a new order"
                >
                    New Order ({countdown}s)
                </button>
            </div>
        </div>
    );
}
