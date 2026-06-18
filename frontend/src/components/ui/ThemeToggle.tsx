import { Moon, Sun } from 'lucide-react'
import { useState } from 'react'
import { cn } from '../../lib/cn'

type Theme = 'dark' | 'light'

function getCurrentTheme(): Theme {
  return document.documentElement.dataset.theme === 'light' ? 'light' : 'dark'
}

export function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>(getCurrentTheme)

  function toggleTheme() {
    const nextTheme = theme === 'dark' ? 'light' : 'dark'

    document.documentElement.dataset.theme = nextTheme
    localStorage.setItem('crm-theme', nextTheme)
    setTheme(nextTheme)
  }

  const isLight = theme === 'light'

  return (
    <button
      type="button"
      onClick={toggleTheme}
      className={cn(
        'inline-flex h-10 items-center gap-2 rounded-xl border px-3 text-sm font-semibold transition',
        'border-[var(--crm-border)] bg-[var(--crm-surface-glass)] text-[var(--crm-text)]',
        'hover:border-cyan-300/40 hover:shadow-[0_0_24px_rgba(65,192,242,0.18)]',
      )}
      aria-label="Toggle theme"
    >
      {isLight ? <Sun size={17} /> : <Moon size={17} />}
      {isLight ? 'Light' : 'Dark'}
    </button>
  )
}