function AdminDashboard() {
  return (
    <div className="h-screen flex flex-col">
      <header className="bg-gray-800 text-white p-4 shadow-lg">
        <h1 className="text-2xl font-bold">Admin Dashboard</h1>
      </header>
      
      <div className="flex-1 p-6 overflow-y-auto">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
          {/* Stats Cards */}
          <div className="card">
            <h3 className="text-gray-600 text-sm font-semibold mb-2">Today's Sales</h3>
            <p className="text-3xl font-bold text-primary-600">$2,543.00</p>
            <p className="text-sm text-green-600 mt-1">↑ 12% from yesterday</p>
          </div>
          
          <div className="card">
            <h3 className="text-gray-600 text-sm font-semibold mb-2">Orders</h3>
            <p className="text-3xl font-bold text-primary-600">47</p>
            <p className="text-sm text-gray-600 mt-1">8 in progress</p>
          </div>
          
          <div className="card">
            <h3 className="text-gray-600 text-sm font-semibold mb-2">Avg Order Value</h3>
            <p className="text-3xl font-bold text-primary-600">$54.10</p>
            <p className="text-sm text-green-600 mt-1">↑ 5% from last week</p>
          </div>
        </div>

        <div className="card">
          <h2 className="text-xl font-semibold mb-4">Recent Orders</h2>
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-2 text-left">Order #</th>
                  <th className="px-4 py-2 text-left">Table</th>
                  <th className="px-4 py-2 text-left">Status</th>
                  <th className="px-4 py-2 text-left">Total</th>
                  <th className="px-4 py-2 text-left">Time</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-t">
                  <td className="px-4 py-2">001</td>
                  <td className="px-4 py-2">Table 5</td>
                  <td className="px-4 py-2">
                    <span className="bg-green-100 text-green-800 px-2 py-1 rounded text-sm">
                      Completed
                    </span>
                  </td>
                  <td className="px-4 py-2">$45.50</td>
                  <td className="px-4 py-2">5 min ago</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  )
}

export default AdminDashboard
