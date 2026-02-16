export default function MenuItemCard({ item, onAdd }) {
    return (
        <div
            onClick={() => onAdd(item)}
            className="card text-center cursor-pointer hover:shadow-lg transition-shadow"
        >
            <div className="mb-1">
                <span className="inline-block bg-gray-100 text-gray-600 text-xs px-2 py-0.5 rounded">
                    {item.kitchenSection}
                </span>
            </div>
            <h3 className="font-semibold text-gray-900">{item.name}</h3>
            {item.description && (
                <p className="text-gray-500 text-sm mt-1 line-clamp-2">{item.description}</p>
            )}
            <p className="text-primary-600 font-bold mt-2">${item.basePrice.toFixed(2)}</p>
        </div>
    );
}
