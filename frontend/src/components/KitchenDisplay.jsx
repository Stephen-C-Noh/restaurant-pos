import { useState, useEffect } from 'react';
import axios from 'axios';

function KitchenDisplay() {
    // 1. Create State for orders
    const [orders, setOrders] = useState([]);

    // 2. Fetch orders when component loads
    useEffect(() =>{
        axios.get('http://localhost:8090/api/orders/active')
        .then(response => {
            console.log(response.data);
            setOrders(response.data);
            })
        .catch(error => console.log(error));
        }, []);

    return (
    <div className="h-screen flex flex-col bg-gray-900 text-white">
      <header className="bg-gray-800 p-4 shadow-lg">
        <h1 className="text-2xl font-bold">Kitchen Display System</h1>
      </header>

      <div className="flex-1 p-4 overflow-y-auto">
        <div className="grid grid-cols-3 gap-4">
          {orders.map(order => (
            <div key={order.id} className="bg-gray-800 rounded-lg p-4 border-l-4 border-yellow-500">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <h3 className="text-xl font-bold">{order.orderNumber}</h3>
                  <p className="text-gray-400">Table {order.tableId || 'N/A'}</p>
                </div>
                <span className="bg-yellow-500 text-gray-900 px-3 py-1 rounded-full text-sm font-semibold">
                  FIRED
                </span>
              </div>

              {/* Items will go here next */}
              <div className="space-y-2">
                {order.items.map(item => (
                  <div key={item.id} className="flex justify-between">
                    <span>{item.quantity}x {item.menuItemName}</span>
                    {item.specialInstructions && (
                      <span className="text-gray-400 text-sm">{item.specialInstructions}</span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
        </div>
    </div>
  )
}

export default KitchenDisplay
