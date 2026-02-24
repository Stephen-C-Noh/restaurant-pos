import { useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import PinPad from './PinPad'
import { loginWithPin } from '../services/authService'
import useAuthStore from '../stores/useAuthStore'

function Login() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const navigate = useNavigate()
  const login = useAuthStore((state) => state.login)

  const handleSubmit = useCallback(async (pin) => {
    setLoading(true)
    setError(null)
    try {
      const data = await loginWithPin(pin)
      login(data.token, {
        id: data.userId,
        username: data.username,
        fullName: data.fullName,
        role: data.role,
      })
      if (data.role === 'SERVER') {
        navigate('/pos')
      } else {
        navigate('/admin')
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Invalid PIN. Please try again.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [login, navigate])

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 dark:bg-gray-900">
      <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-lg p-10 w-full max-w-sm">
        <h1 className="text-3xl font-bold text-center mb-2 text-gray-800 dark:text-gray-100">
          Restaurant POS
        </h1>
        <p className="text-center text-gray-500 dark:text-gray-400 text-sm mb-8">
          Enter your PIN to sign in
        </p>

        <PinPad onSubmit={handleSubmit} loading={loading} error={error} />

        {loading && (
          <p className="text-center text-gray-500 dark:text-gray-400 text-sm mt-6">
            Signing in...
          </p>
        )}
      </div>
    </div>
  )
}

export default Login
