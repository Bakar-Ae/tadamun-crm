import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Toaster } from 'react-hot-toast'
import './index.css'
import App from './App.tsx'

const savedTheme = localStorage.getItem('crm-theme')
const systemTheme = window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
const initialTheme = savedTheme === 'light' || savedTheme === 'dark' ? savedTheme : systemTheme

document.documentElement.dataset.theme = initialTheme

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
    <Toaster
      position="top-right"
      toastOptions={{
        duration: 3500,
     style: {
         background: 'var(--crm-surface)',
         color: 'var(--crm-text)',
         border: '1px solid var(--crm-border)',
},
      }}
    />
  </StrictMode>,
)