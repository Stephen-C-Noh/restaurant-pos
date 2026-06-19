import { useState, useEffect } from 'react';
import { fetchAvailableTables, updateTableStatus } from '../../services/tableService';
import useAuthStore from '../../stores/useAuthStore';

export default function TablePicker({ onTableSelected }) {
    const [tables, setTables] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedZone, setSelectedZone] = useState('ALL');
    const [claimingId, setClaimingId] = useState(null);

    const user = useAuthStore((state) => state.user);

    const load = () => {
        setLoading(true);
        setError(null);
        fetchAvailableTables()
            .then((data) => {
                setTables(data);
                setLoading(false);
            })
            .catch((err) => {
                setError(err.response?.data?.message || 'Failed to load tables.');
                setLoading(false);
            });
    };

    useEffect(() => { load(); }, []);

    const zones = ['ALL', ...new Set(tables.map((t) => t.zone).filter(Boolean))];

    const filtered = selectedZone === 'ALL'
        ? tables
        : tables.filter((t) => t.zone === selectedZone);

    const handleSelectTable = async (table) => {
        setClaimingId(table.id);
        try {
            const updated = await updateTableStatus(table.id, 'OCCUPIED', user.id);
            onTableSelected(updated);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to claim table.');
            setClaimingId(null);
        }
    };

    return (
        <div className="h-screen flex flex-col bg-gray-50">
            <header className="bg-primary-600 text-white p-4 shadow-lg flex items-center justify-between">
                <h1 className="text-2xl font-bold">Select a Table</h1>
                <button
                    onClick={() => onTableSelected(null)}
                    className="bg-white text-primary-600 font-semibold px-4 py-2 rounded-lg hover:bg-gray-100 text-sm"
                >
                    Takeout / Delivery
                </button>
            </header>

            <div className="flex-1 p-4 overflow-y-auto">
                {loading ? (
                    <div className="flex items-center justify-center h-full">
                        <p className="text-gray-500 text-lg">Loading tables...</p>
                    </div>
                ) : error ? (
                    <div className="flex flex-col items-center justify-center h-full">
                        <p className="text-red-500 mb-4" role="alert">{error}</p>
                        <button onClick={load} className="btn-primary">Retry</button>
                    </div>
                ) : tables.length === 0 ? (
                    <div className="flex items-center justify-center h-full">
                        <p className="text-gray-500 text-lg">No tables available — use Takeout/Delivery</p>
                    </div>
                ) : (
                    <>
                        <div className="flex gap-2 mb-4 flex-wrap">
                            {zones.map((zone) => (
                                <button
                                    key={zone}
                                    onClick={() => setSelectedZone(zone)}
                                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                                        selectedZone === zone
                                            ? 'bg-primary-600 text-white'
                                            : 'bg-white text-gray-700 hover:bg-gray-100 border'
                                    }`}
                                >
                                    {zone}
                                </button>
                            ))}
                        </div>

                        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                            {filtered.map((table) => (
                                <button
                                    key={table.id}
                                    onClick={() => handleSelectTable(table)}
                                    disabled={claimingId === table.id}
                                    className="bg-white border-2 border-green-400 rounded-xl p-4 text-left hover:shadow-md transition-shadow disabled:opacity-60"
                                >
                                    <div className="flex justify-between items-start mb-2">
                                        <span className="text-lg font-bold text-gray-900">
                                            {table.tableNumber}
                                        </span>
                                        <span className="bg-green-100 text-green-800 text-xs font-semibold px-2 py-0.5 rounded">
                                            Available
                                        </span>
                                    </div>
                                    <p className="text-sm text-gray-500">
                                        {table.capacity} seats
                                    </p>
                                    {table.zone && (
                                        <p className="text-xs text-gray-400 mt-1">
                                            {table.zone}
                                        </p>
                                    )}
                                    {claimingId === table.id && (
                                        <p className="text-xs text-primary-600 mt-2 animate-pulse">
                                            Claiming...
                                        </p>
                                    )}
                                </button>
                            ))}
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
