import { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

function KitchenDisplay() {
    // Create State for orders
    const [orders, setOrders] = useState([]);
    const [previousOrderCount, setPreviousOrderCount] = useState(0); // Track Count
    const [isMonitoring, setIsMonitoring] = useState(false);

    // Audio for new Orders
    const notificationSound = useRef(new Audio('data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBTGH0fPTgjMGHm7A7+OZURE'));

    const fetchOrders = () => {
        axios.get('http://localhost:8090/api/orders/active')
            .then(response => {
                const newOrders = response.data;

                setOrders(newOrders);

                // Update count and check for new orders in one step
                setPreviousOrderCount(prev => {
                    if (prev > 0 && newOrders.length > prev) {
                        notificationSound.current.currentTime = 0;
                        notificationSound.current.play()
                            .catch(err => console.error('Sound error:', err));
                    }
                    return newOrders.length;
                });
            })
            .catch(error => console.log(error));
    };
    useEffect(() => {
        // Load orders initially
        fetchOrders();
}, []);

    // WebSocket Connection
    useEffect(() =>{
        // Create WebSocket Client
        const client = new Client({
            webSocketFactory:  () => new SockJS('http://localhost:8090/ws'),
            onConnect: () => {
                console.log('WebSocket Connected.');

                // Subscribe to order updates
                client.subscribe('/topic/orders', (message) =>{
                    console.log('Received Order: ', message.body);
                    const newOrder = JSON.parse(message.body);

                    // Add or Update order in state
                    setOrders(prevOrders =>{
                        const exists = prevOrders.find(o => o.id === newOrder.id);
                        if (exists){
                            // Update existing order
                            return prevOrders.map(o => o.id === newOrder.id ? newOrder : o);
                        }else{
                            // Add new order and play notificationSound
                            setPreviousOrderCount(prev => prev + 1);
                            if(notificationSound.current){
                                notificationSound.current.currentTime = 0;
                                notificationSound.current.play().catch(e => console.log(e));
                            }
                            return [...prevOrders, newOrder];
                        }
                    });
                    });
                },
            onDisconnect: () => {
                console.log('WebSocket Disconnected');
                }
            });

        client.activate();

        // Clean up on unmount
        return () => client.deactivate();
       }, []);

    const getOrderAge = (firedAt) => {
        const now = new Date();
        const firedTime = new Date(firedAt);
        const diffMs = now - firedTime;
        const diffMins = Math.floor(diffMs/1000/60);
        return diffMins;
        };

    const getBorderColor = (age) =>{
        if (age> 20) return 'border-red-500';
        if (age> 15) return 'border-orange-500';
        return 'border-yellow-500';
        };

    return (
    <div className="h-screen flex flex-col bg-gray-900 text-white">
      <header className="bg-gray-800 p-4 shadow-lg">
        <div className="flex justify-between items-center">
          <h1 className="text-2xl font-bold">Kitchen Display System</h1>
          {!isMonitoring && (
            <button
              onClick={() => {
                setIsMonitoring(true);
                notificationSound.current.play().then(() => {
                  notificationSound.current.pause();
                  notificationSound.current.currentTime = 0;
                });
              }}
              className="bg-green-600 hover:bg-green-700 px-4 py-2 rounded font-semibold"
            >
              🔊 Enable Sound Alerts
            </button>
          )}
          {isMonitoring && (
            <span className="text-green-400">✓ Sound Alerts Active</span>
          )}
        </div>
      </header>

      <div className="flex-1 p-4 overflow-y-auto">
        <div className="grid grid-cols-3 gap-4">
          {orders.map(order => {
              const age = getOrderAge(order.firedAt);
              const borderColor = getBorderColor(age);

              return (
                <div key={order.id} className={`bg-gray-800 rounded-lg p-4 border-l-4 ${borderColor}`}>
                  <div className="flex justify-between items-start mb-4">
                    <div>
                      <h3 className="text-xl font-bold">{order.orderNumber}</h3>
                      <p className="text-gray-400">Table {order.tableId || 'N/A'} • {age} min</p>
                    </div>
                    <span className="bg-yellow-500 text-gray-900 px-3 py-1 rounded-full text-sm font-semibold">
                      FIRED
                    </span>
                  </div>

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
              );
          })}
        </div>
        </div>
    </div>
  )
}

export default KitchenDisplay
