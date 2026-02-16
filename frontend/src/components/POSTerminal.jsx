import useMenu from '../hooks/useMenu';
import useOrderStore from '../stores/useOrderStore';
import CategoryTabs from './pos/CategoryTabs';
import MenuGrid from './pos/MenuGrid';
import CartPanel from './pos/CartPanel';
import OrderConfirmation from './pos/OrderConfirmation';

function POSTerminal() {
    const {
        filteredItems,
        sections,
        loading,
        error,
        selectedSection,
        setSelectedSection,
        retry,
    } = useMenu();

    const { lastOrder, addItem } = useOrderStore();

    return (
        <div className="h-screen flex flex-col">
            <header className="bg-primary-600 text-white p-4 shadow-lg">
                <h1 className="text-2xl font-bold">POS Terminal</h1>
            </header>

            <div className="flex-1 flex overflow-hidden">
                {/* Menu Section */}
                <div className="flex-1 p-4 overflow-y-auto bg-gray-50">
                    {loading ? (
                        <div className="flex items-center justify-center h-full">
                            <p className="text-gray-500 text-lg">Loading menu...</p>
                        </div>
                    ) : error ? (
                        <div className="flex flex-col items-center justify-center h-full">
                            <p className="text-red-500 mb-4" role="alert">{error}</p>
                            <button onClick={retry} className="btn-primary">
                                Retry
                            </button>
                        </div>
                    ) : (
                        <>
                            <CategoryTabs
                                sections={sections}
                                selectedSection={selectedSection}
                                onSelect={setSelectedSection}
                            />
                            <MenuGrid items={filteredItems} onAddItem={addItem} />
                        </>
                    )}
                </div>

                {/* Right Panel: Cart or Confirmation */}
                {lastOrder ? <OrderConfirmation /> : <CartPanel />}
            </div>
        </div>
    );
}

export default POSTerminal;
