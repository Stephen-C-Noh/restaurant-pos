import { useState } from 'react';
import useTables from '../hooks/useTables';
import { updateTableStatus, createTable, deleteTable } from '../services/tableService';
import useAuthStore from '../stores/useAuthStore';

const STATUS_COLORS = {
    AVAILABLE: { border: 'border-l-green-500', badge: 'bg-green-100 text-green-800' },
    OCCUPIED: { border: 'border-l-red-500', badge: 'bg-red-100 text-red-800' },
    RESERVED: { border: 'border-l-yellow-500', badge: 'bg-yellow-100 text-yellow-800' },
    CLEANING: { border: 'border-l-blue-500', badge: 'bg-blue-100 text-blue-800' },
};

function getAction(status, role) {
    switch (status) {
        case 'AVAILABLE':
            return (role === 'ADMIN' || role === 'MANAGER')
                ? { label: 'Mark Reserved', next: 'RESERVED' }
                : null;
        case 'OCCUPIED':
            return { label: 'Mark Cleaning', next: 'CLEANING' };
        case 'RESERVED':
            return { label: 'Mark Available', next: 'AVAILABLE' };
        case 'CLEANING':
            return { label: 'Mark Available', next: 'AVAILABLE' };
        default:
            return null;
    }
}

export default function Tables() {
    const { tables, loading, error, refresh } = useTables();
    const user = useAuthStore((state) => state.user);
    const [selectedZone, setSelectedZone] = useState('ALL');
    const [actionLoading, setActionLoading] = useState(null);
    const [showAddModal, setShowAddModal] = useState(false);
    const [formData, setFormData] = useState({ tableNumber: '', capacity: '', zone: '' });
    const [formError, setFormError] = useState(null);
    const [formSubmitting, setFormSubmitting] = useState(false);

    const zones = ['ALL', ...new Set(tables.map((t) => t.zone).filter(Boolean))];
    const filtered = selectedZone === 'ALL' ? tables : tables.filter((t) => t.zone === selectedZone);

    const handleAction = async (tableId, newStatus) => {
        setActionLoading(tableId);
        try {
            await updateTableStatus(tableId, newStatus);
            refresh();
        } catch {
            // refresh to show current state
            refresh();
        } finally {
            setActionLoading(null);
        }
    };

    const handleDelete = async (tableId) => {
        setActionLoading(tableId);
        try {
            await deleteTable(tableId);
            refresh();
        } catch {
            refresh();
        } finally {
            setActionLoading(null);
        }
    };

    const handleAddTable = async (e) => {
        e.preventDefault();
        setFormSubmitting(true);
        setFormError(null);
        try {
            await createTable(formData.tableNumber, parseInt(formData.capacity, 10), formData.zone);
            setShowAddModal(false);
            setFormData({ tableNumber: '', capacity: '', zone: '' });
            refresh();
        } catch (err) {
            setFormError(err.response?.data?.message || 'Failed to create table.');
        } finally {
            setFormSubmitting(false);
        }
    };

    return (
        <div className="h-screen flex flex-col bg-gray-50">
            <header className="bg-primary-600 text-white p-4 shadow-lg flex items-center justify-between">
                <h1 className="text-2xl font-bold">Table Management</h1>
                {user?.role === 'ADMIN' && (
                    <button
                        onClick={() => setShowAddModal(true)}
                        className="bg-white text-primary-600 font-semibold px-4 py-2 rounded-lg hover:bg-gray-100 text-sm"
                    >
                        Add Table
                    </button>
                )}
            </header>

            <div className="flex-1 p-4 overflow-y-auto">
                {loading ? (
                    <div className="flex items-center justify-center h-full">
                        <p className="text-gray-500 text-lg">Loading tables...</p>
                    </div>
                ) : error ? (
                    <div className="flex flex-col items-center justify-center h-full">
                        <p className="text-red-500 mb-4" role="alert">{error}</p>
                        <button onClick={refresh} className="btn-primary">Retry</button>
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

                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            {filtered.map((table) => {
                                const colors = STATUS_COLORS[table.status] || STATUS_COLORS.AVAILABLE;
                                const action = getAction(table.status, user?.role);
                                const isLoading = actionLoading === table.id;

                                return (
                                    <div
                                        key={table.id}
                                        className={`bg-white rounded-lg border-l-4 ${colors.border} p-4 shadow-sm`}
                                    >
                                        <div className="flex justify-between items-start mb-2">
                                            <span className="text-lg font-bold text-gray-900">
                                                {table.tableNumber}
                                            </span>
                                            <span className={`text-xs font-semibold px-2 py-0.5 rounded ${colors.badge}`}>
                                                {table.status}
                                            </span>
                                        </div>
                                        <p className="text-sm text-gray-500">
                                            {table.capacity} seats
                                        </p>
                                        {table.zone && (
                                            <p className="text-xs text-gray-400 mt-1">{table.zone}</p>
                                        )}
                                        {table.status === 'OCCUPIED' && table.serverName && (
                                            <p className="text-xs text-gray-500 mt-1">
                                                Server: {table.serverName}
                                            </p>
                                        )}

                                        <div className="flex gap-2 mt-3">
                                            {action && (
                                                <button
                                                    onClick={() => handleAction(table.id, action.next)}
                                                    disabled={isLoading}
                                                    className="btn-primary text-xs px-3 py-1 disabled:opacity-50"
                                                >
                                                    {isLoading ? 'Updating...' : action.label}
                                                </button>
                                            )}
                                            {user?.role === 'ADMIN' && table.status !== 'OCCUPIED' && (
                                                <button
                                                    onClick={() => handleDelete(table.id)}
                                                    disabled={isLoading}
                                                    className="text-xs px-3 py-1 rounded bg-red-50 text-red-600 hover:bg-red-100 disabled:opacity-50"
                                                >
                                                    Delete
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </>
                )}
            </div>

            {showAddModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
                        <h2 className="text-xl font-bold text-gray-900 mb-4">Add Table</h2>
                        <form onSubmit={handleAddTable} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Table Number
                                </label>
                                <input
                                    type="text"
                                    required
                                    value={formData.tableNumber}
                                    onChange={(e) => setFormData({ ...formData, tableNumber: e.target.value })}
                                    className="w-full border rounded-lg px-3 py-2 text-gray-900"
                                    placeholder="e.g. T01"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Capacity
                                </label>
                                <input
                                    type="number"
                                    required
                                    min="1"
                                    value={formData.capacity}
                                    onChange={(e) => setFormData({ ...formData, capacity: e.target.value })}
                                    className="w-full border rounded-lg px-3 py-2 text-gray-900"
                                    placeholder="e.g. 4"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Zone
                                </label>
                                <input
                                    type="text"
                                    required
                                    value={formData.zone}
                                    onChange={(e) => setFormData({ ...formData, zone: e.target.value })}
                                    className="w-full border rounded-lg px-3 py-2 text-gray-900"
                                    placeholder="e.g. Main Dining"
                                />
                            </div>
                            {formError && (
                                <p className="text-red-500 text-sm" role="alert">{formError}</p>
                            )}
                            <div className="flex gap-3 justify-end">
                                <button
                                    type="button"
                                    onClick={() => { setShowAddModal(false); setFormError(null); }}
                                    className="px-4 py-2 text-gray-600 hover:text-gray-800 text-sm"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    disabled={formSubmitting}
                                    className="btn-primary px-4 py-2 text-sm disabled:opacity-50"
                                >
                                    {formSubmitting ? 'Creating...' : 'Create Table'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
