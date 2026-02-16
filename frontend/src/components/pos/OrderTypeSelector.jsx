const ORDER_TYPES = [
    { value: 'DINE_IN', label: 'Dine In' },
    { value: 'TAKEOUT', label: 'Takeout' },
    { value: 'DELIVERY', label: 'Delivery' },
];

export default function OrderTypeSelector({ selected, onSelect }) {
    return (
        <div className="flex gap-2 mb-4">
            {ORDER_TYPES.map((type) => (
                <button
                    key={type.value}
                    onClick={() => onSelect(type.value)}
                    className={`flex-1 py-2 rounded text-sm font-semibold transition-colors ${
                        selected === type.value
                            ? 'bg-primary-600 text-white'
                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                >
                    {type.label}
                </button>
            ))}
        </div>
    );
}
