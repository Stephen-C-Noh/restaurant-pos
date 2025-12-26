import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import POSTerminal from './components/POSTerminal'
import KitchenDisplay from './components/KitchenDisplay'
import AdminDashboard from './components/AdminDashboard'
import Login from './components/Login'

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-gray-100">
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/login" element={<Login />} />
          <Route path="/pos" element={<POSTerminal />} />
          <Route path="/kds" element={<KitchenDisplay />} />
          <Route path="/admin" element={<AdminDashboard />} />
        </Routes>
      </div>
    </Router>
  )
}

export default App