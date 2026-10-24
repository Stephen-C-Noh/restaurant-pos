import { useState, useCallback } from 'react';
import useMenu from '../hooks/useMenu';
import useOrderStore from '../stores/useOrderStore';
import CategoryTabs from './pos/CategoryTabs';
import MenuGrid from './pos/MenuGrid';
import CartPanel from './pos/CartPanel';
import OrderConfirmation from './pos/OrderConfirmation';
import TablePicker from './pos/TablePicker';

function POSTerminal() {
    const [selectedTable, setSelectedTable] = useState(null);
    const [tableSelectionDone, setTableSelectionDone] = useState(false);

    const { filteredItems, sections, loading, error, selectedSection, setSelectedSection, retry } = useMenu();
    const { lastOrder, addItem, setOrderType, resetOrder } = useOrderStore();

    const handleTableSelected = (table) => {
        setSelectedTable(table);
        setTableSelectionDone(true);
        if (!table) setOrderType('TAKEOUT');
    };

    const handleReset = useCallback(() => {
        resetOrder();
        setSelectedTable(null);
        setTableSelectionDone(false);
    }, [resetOrder]);

    // Show table picker if table hasn't been selected and no order is showing
    if (!tableSelectionDone && !lastOrder) {
        return <TablePicker onTableSelected={handleTableSelected} />;
    }

    return (
        <div className="h-screen flex flex-col">
            <header className="bg-primary-600 text-white p-4 shadow-lg flex items-center justify-between">
                <h1 className="text-2xl font-bold">POS Terminal</h1>
                {selectedTable && (
                    <div className="flex items-center gap-3">
                        <span className="bg-white text-primary-600 text-sm font-semibold px-3 py-1 rounded-full">
                            Table {selectedTable.tableNumber}
                        </span>
                        <button
                            onClick={handleReset}
                            className="text-white/70 hover:text-white text-sm underline"
                        >
                            Change Table
                        </button>
                    </div>
                )}
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
                            <button onClick={retry} className="btn-primary">Retry</button>
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

                {/* Right Panel */}
                {lastOrder
                    ? <OrderConfirmation onReset={handleReset} />
                    : <CartPanel selectedTable={selectedTable} />
                }
            </div>
        </div>
    );
}

export default POSTerminal;
