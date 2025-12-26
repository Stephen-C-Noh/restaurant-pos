import { useState } from 'react'

function POSTerminal() {
  const [currentOrder, setCurrentOrder] = useState(null)

  return (
    <div className="h-screen flex flex-col">
      <header className="bg-primary-600 text-white p-4 shadow-lg">
        <h1 className="text-2xl font-bold">POS Terminal</h1>
      </header>
      
      <div className="flex-1 flex">
        {/* Menu Items Section */}
        <div className="flex-1 p-4 overflow-y-auto">
          <h2 className="text-xl font-semibold mb-4">Menu</h2>
          <div className="grid grid-cols-3 gap-4">
            {/* Menu items will go here */}
            <div className="card text-center cursor-pointer hover:shadow-lg transition-shadow">
              <div className="text-4xl mb-2">🍔</div>
              <h3 className="font-semibold">Burger</h3>
              <p className="text-gray-600">$12.99</p>
            </div>
            <div className="card text-center cursor-pointer hover:shadow-lg transition-shadow">
              <div className="text-4xl mb-2">🍕</div>
              <h3 className="font-semibold">Pizza</h3>
              <p className="text-gray-600">$14.99</p>
            </div>
            <div className="card text-center cursor-pointer hover:shadow-lg transition-shadow">
              <div className="text-4xl mb-2">🥗</div>
              <h3 className="font-semibold">Salad</h3>
              <p className="text-gray-600">$9.99</p>
            </div>
          </div>
        </div>

        {/* Order Summary Section */}
        <div className="w-96 bg-white border-l shadow-lg p-4">
          <h2 className="text-xl font-semibold mb-4">Current Order</h2>
          <div className="flex-1 mb-4">
            <p className="text-gray-500 text-center py-8">No items in order</p>
          </div>
          
          <div className="border-t pt-4">
            <div className="flex justify-between mb-2">
              <span>Subtotal:</span>
              <span>$0.00</span>
            </div>
            <div className="flex justify-between mb-2">
              <span>Tax:</span>
              <span>$0.00</span>
            </div>
            <div className="flex justify-between font-bold text-lg">
              <span>Total:</span>
              <span>$0.00</span>
            </div>
          </div>

          <button className="btn-primary w-full mt-4">
            Send to Kitchen
          </button>
        </div>
      </div>
    </div>
  )
}

export default POSTerminal
