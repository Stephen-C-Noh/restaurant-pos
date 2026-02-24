import { useState, useEffect } from 'react'

function PinPad({ onSubmit, loading, error }) {
  const [digits, setDigits] = useState([])

  useEffect(() => {
    if (digits.length === 4) {
      onSubmit(digits.join(''))
      setDigits([])
    }
  }, [digits, onSubmit])

  const handleDigit = (d) => {
    if (loading || digits.length >= 4) return
    setDigits((prev) => [...prev, d])
  }

  const handleBackspace = () => {
    if (loading) return
    setDigits((prev) => prev.slice(0, -1))
  }

  const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', null, '0', 'back']

  return (
    <div className="flex flex-col items-center gap-6">
      {/* PIN dots */}
      <div className="flex gap-4">
        {[0, 1, 2, 3].map((i) => (
          <div
            key={i}
            className={`w-4 h-4 rounded-full border-2 transition-colors ${
              i < digits.length
                ? 'bg-blue-600 border-blue-600 dark:bg-blue-400 dark:border-blue-400'
                : 'border-gray-400 dark:border-gray-500'
            }`}
          />
        ))}
      </div>

      {/* Error message */}
      {error && (
        <p className="text-red-500 dark:text-red-400 text-sm font-medium">{error}</p>
      )}

      {/* Keypad */}
      <div className="grid grid-cols-3 gap-3">
        {keys.map((key, idx) => {
          if (key === null) {
            return <div key={idx} />
          }
          if (key === 'back') {
            return (
              <button
                key={idx}
                onClick={handleBackspace}
                disabled={loading}
                className="w-16 h-16 rounded-xl bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-200
                           flex items-center justify-center text-xl font-semibold
                           hover:bg-gray-300 dark:hover:bg-gray-600 active:scale-95 transition-all
                           disabled:opacity-50 disabled:cursor-not-allowed"
                aria-label="Backspace"
              >
                ←
              </button>
            )
          }
          return (
            <button
              key={key}
              onClick={() => handleDigit(key)}
              disabled={loading || digits.length >= 4}
              className="w-16 h-16 rounded-xl bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-100
                         border border-gray-200 dark:border-gray-600
                         flex items-center justify-center text-2xl font-semibold
                         hover:bg-gray-50 dark:hover:bg-gray-700 active:scale-95 transition-all
                         shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {key}
            </button>
          )
        })}
      </div>
    </div>
  )
}

export default PinPad
