import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Toaster } from 'react-hot-toast'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
    <Toaster
      position="top-right"
      toastOptions={{
        duration: 3500,
        style: {
          background: '#07191e',
          color: '#f8fafc',
          border: '1px solid rgba(173, 223, 241, 0.16)',
        },
      }}
    />
  </StrictMode>,
)