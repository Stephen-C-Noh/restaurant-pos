import { useEffect, useState } from 'react';
import useAdminDashboard from '../hooks/useAdminDashboard';
import {
    BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer
} from 'recharts';

function formatCurrency(value) {
    if (value == null) return '$0.00';
    return '$' + Number(value).toFixed(2);
}

function timeAgo(isoString) {
    const now = Date.now();
    const then = new Date(isoString).getTime();
    const diffMs = now - then;
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    return `${Math.floor(diffHours / 24)}d ago`;
}

const STATUS_COLORS = {
    DRAFT:      'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
    SUBMITTED:  'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300',
    FIRED:      'bg-orange-100 text-orange-700 dark:bg-orange-900 dark:text-orange-300',
    PREPARING:  'bg-yellow-100 text-yellow-700 dark:bg-yellow-900 dark:text-yellow-300',
    READY:      'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300',
    COMPLETED:  'bg-green-200 text-green-800 dark:bg-green-900 dark:text-green-300',
    CANCELLED:  'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300',
};

function StatCard({ label, value, sub }) {
    return (
        <div className="card">
            <h3 className="text-gray-500 dark:text-gray-400 text-sm font-semibold mb-2">{label}</h3>
            <p className="text-3xl font-bold text-primary-600 dark:text-primary-500">{value}</p>
            {sub && <p className="text-sm text-gray-400 dark:text-gray-500 mt-1">{sub}</p>}
        </div>
    );
}

function LoadingSkeleton() {
    return (
        <div className="animate-pulse space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4">
                {Array.from({ length: 5 }).map((_, i) => (
                    <div key={i} className="card h-24 bg-gray-200 dark:bg-gray-700 rounded" />
                ))}
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="card h-64 bg-gray-200 dark:bg-gray-700 rounded" />
                <div className="card h-64 bg-gray-200 dark:bg-gray-700 rounded" />
            </div>
            <div className="card h-48 bg-gray-200 dark:bg-gray-700 rounded" />
        </div>
    );
}

function AdminDashboard() {
    const { stats, loading, error, retry } = useAdminDashboard();
    const [dark, setDark] = useState(() => localStorage.getItem('adminTheme') === 'dark');

    useEffect(() => {
        const root = document.documentElement;
        if (dark) {
            root.classList.add('dark');
            localStorage.setItem('adminTheme', 'dark');
        } else {
            root.classList.remove('dark');
            localStorage.setItem('adminTheme', 'light');
        }
    }, [dark]);

    const tickColor = dark ? '#9ca3af' : '#6b7280';
    const tooltipStyle = dark
        ? { backgroundColor: '#1f2937', border: '1px solid #374151', color: '#f9fafb' }
        : undefined;

    return (
        <div className="h-screen flex flex-col bg-gray-50 dark:bg-gray-900">
            <header className="bg-gray-800 dark:bg-gray-950 text-white p-4 shadow-lg flex items-center justify-between">
                <h1 className="text-2xl font-bold">Admin Dashboard</h1>
                <div className="flex items-center gap-3">
                    <button
                        onClick={() => setDark(d => !d)}
                        className="text-sm bg-gray-700 hover:bg-gray-600 px-3 py-1 rounded"
                    >
                        {dark ? '☀ Light' : '☾ Dark'}
                    </button>
                    <button
                        onClick={retry}
                        className="text-sm bg-gray-700 hover:bg-gray-600 px-3 py-1 rounded"
                    >
                        Refresh
                    </button>
                </div>
            </header>

            <div className="flex-1 p-6 overflow-y-auto">
                {loading && <LoadingSkeleton />}

                {error && !loading && (
                    <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 rounded p-4 mb-6">
                        {error}
                        <button onClick={retry} className="ml-4 underline text-sm">Retry</button>
                    </div>
                )}

                {stats && !loading && (
                    <>
                        {/* Stat Cards */}
                        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4 mb-6">
                            <StatCard label="Today's Revenue" value={formatCurrency(stats.todayRevenue)} />
                            <StatCard label="Today's Orders" value={stats.todayOrderCount} />
                            <StatCard label="Active Orders" value={stats.activeOrderCount} sub="Fired / Preparing / Ready" />
                            <StatCard label="Avg Order Value" value={formatCurrency(stats.avgOrderValue)} />
                            <StatCard label="Avg Revenue / Table" value={formatCurrency(stats.avgRevenuePerTable)} sub="Dine-in only" />
                        </div>

                        {/* Charts */}
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                            <div className="card">
                                <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Orders per Hour</h2>
                                {stats.ordersByHour.every(h => h.orderCount === 0) ? (
                                    <p className="text-gray-400 text-sm">No orders today yet.</p>
                                ) : (
                                    <ResponsiveContainer width="100%" height={220}>
                                        <BarChart data={stats.ordersByHour}>
                                            <XAxis dataKey="label" tick={{ fontSize: 11, fill: tickColor }} />
                                            <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: tickColor }} />
                                            <Tooltip contentStyle={tooltipStyle} />
                                            <Bar dataKey="orderCount" name="Orders" fill="#4f46e5" radius={[4, 4, 0, 0]} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                )}
                            </div>

                            <div className="card">
                                <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Top Selling Items</h2>
                                {stats.topItems.length === 0 ? (
                                    <p className="text-gray-400 text-sm">No sales today yet.</p>
                                ) : (
                                    <ResponsiveContainer width="100%" height={220}>
                                        <BarChart layout="vertical" data={stats.topItems}>
                                            <XAxis type="number" allowDecimals={false} tick={{ fontSize: 11, fill: tickColor }} />
                                            <YAxis dataKey="name" type="category" width={130} tick={{ fontSize: 11, fill: tickColor }} />
                                            <Tooltip contentStyle={tooltipStyle} />
                                            <Bar dataKey="quantitySold" name="Qty Sold" fill="#10b981" radius={[0, 4, 4, 0]} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                )}
                            </div>
                        </div>

                        {/* Recent Orders */}
                        <div className="card">
                            <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-4">Recent Orders</h2>
                            {stats.recentOrders.length === 0 ? (
                                <p className="text-gray-400 text-sm">No orders yet.</p>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="min-w-full">
                                        <thead className="bg-gray-50 dark:bg-gray-700">
                                            <tr>
                                                <th className="px-4 py-2 text-left text-sm font-semibold text-gray-600 dark:text-gray-300">Order #</th>
                                                <th className="px-4 py-2 text-left text-sm font-semibold text-gray-600 dark:text-gray-300">Type</th>
                                                <th className="px-4 py-2 text-left text-sm font-semibold text-gray-600 dark:text-gray-300">Status</th>
                                                <th className="px-4 py-2 text-left text-sm font-semibold text-gray-600 dark:text-gray-300">Total</th>
                                                <th className="px-4 py-2 text-left text-sm font-semibold text-gray-600 dark:text-gray-300">Time</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {stats.recentOrders.map((order) => (
                                                <tr key={order.orderNumber} className="border-t border-gray-100 dark:border-gray-700">
                                                    <td className="px-4 py-2 font-mono text-sm text-gray-900 dark:text-gray-100">{order.orderNumber}</td>
                                                    <td className="px-4 py-2 text-sm capitalize text-gray-900 dark:text-gray-100">
                                                        {order.orderType?.replace('_', ' ').toLowerCase()}
                                                    </td>
                                                    <td className="px-4 py-2">
                                                        <span className={`px-2 py-1 rounded text-xs font-medium ${STATUS_COLORS[order.status] ?? STATUS_COLORS.DRAFT}`}>
                                                            {order.status}
                                                        </span>
                                                    </td>
                                                    <td className="px-4 py-2 text-sm text-gray-900 dark:text-gray-100">{formatCurrency(order.total)}</td>
                                                    <td className="px-4 py-2 text-sm text-gray-500 dark:text-gray-400">{timeAgo(order.createdAt)}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}

export default AdminDashboard;
