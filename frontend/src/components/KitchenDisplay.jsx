function KitchenDisplay() {
  return (
    <div className="h-screen flex flex-col bg-gray-900 text-white">
      <header className="bg-gray-800 p-4 shadow-lg">
        <h1 className="text-2xl font-bold">Kitchen Display System</h1>
      </header>
      
      <div className="flex-1 p-4 overflow-y-auto">
        <div className="grid grid-cols-3 gap-4">
          {/* Sample order card */}
          <div className="bg-gray-800 rounded-lg p-4 border-l-4 border-yellow-500">
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="text-xl font-bold">Order #001</h3>
                <p className="text-gray-400">Table 5 • 2 min ago</p>
              </div>
              <span className="bg-yellow-500 text-gray-900 px-3 py-1 rounded-full text-sm font-semibold">
                NEW
              </span>
            </div>
            
            <div className="space-y-2">
              <div className="flex justify-between">
                <span>1x Burger</span>
                <span className="text-gray-400">Well done</span>
              </div>
              <div className="flex justify-between">
                <span>1x Fries</span>
                <span className="text-gray-400">Extra crispy</span>
              </div>
            </div>

            <button className="w-full mt-4 bg-green-600 hover:bg-green-700 text-white font-semibold py-2 rounded transition-colors">
              Mark Ready
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default KitchenDisplay
