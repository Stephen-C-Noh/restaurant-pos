import MenuItemCard from './MenuItemCard';

export default function MenuGrid({ items, onAddItem }) {
    if (items.length === 0) {
        return (
            <div className="text-center py-12 text-gray-500">
                No menu items available for this section.
            </div>
        );
    }

    return (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {items.map((item) => (
                <MenuItemCard key={item.id} item={item} onAdd={onAddItem} />
            ))}
        </div>
    );
}
