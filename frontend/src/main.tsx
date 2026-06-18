import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { Toaster } from 'react-hot-toast'
import './index.css'
import App from './App.tsx'

const savedTheme = localStorage.getItem('crm-theme')
const initialTheme =
  savedTheme === 'midnight' || savedTheme === 'dark'
    ? 'midnight'
    : 'luxe'
const savedDashboardPreferences = localStorage.getItem('crm-dashboard-preferences')

document.documentElement.dataset.theme = initialTheme
localStorage.setItem('crm-theme', initialTheme)

if (savedDashboardPreferences) {
  try {
    const preferences = JSON.parse(savedDashboardPreferences) as { density?: string }

    if (preferences.density === 'compact' || preferences.density === 'comfortable') {
      document.documentElement.dataset.density = preferences.density
    }
  } catch {
    localStorage.removeItem('crm-dashboard-preferences')
  }
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
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
