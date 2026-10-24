import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import POSTerminal from './components/POSTerminal'
import KitchenDisplay from './components/KitchenDisplay'
import AdminDashboard from './components/AdminDashboard'
import Tables from './components/Tables'
import Login from './components/Login'
import useAuthStore from './stores/useAuthStore'

function ProtectedRoute({ children, allowedRoles }) {
  const user = useAuthStore((state) => state.user)
  const token = useAuthStore((state) => state.token)

  if (!token || !user) {
    return <Navigate to="/login" replace />
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    const fallback = user.role === 'SERVER' ? '/pos' : '/admin'
    return <Navigate to={fallback} replace />
  }

  return children
}

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-gray-100">
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<Login />} />
          <Route
            path="/pos"
            element={
              <ProtectedRoute allowedRoles={['SERVER', 'ADMIN', 'MANAGER']}>
                <POSTerminal />
              </ProtectedRoute>
            }
          />
          <Route path="/kds" element={<KitchenDisplay />} />
          <Route
            path="/tables"
            element={
              <ProtectedRoute allowedRoles={['SERVER','ADMIN','MANAGER']}>
                <Tables />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'MANAGER']}>
                <AdminDashboard />
              </ProtectedRoute>
            }
          />
        </Routes>
      </div>
    </Router>
  )
}

export default App
