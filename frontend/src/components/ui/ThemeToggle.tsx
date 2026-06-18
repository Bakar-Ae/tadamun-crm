import { Moon, Sparkles } from 'lucide-react'
import { useState } from 'react'
import { cn } from '../../lib/cn'

type Theme = 'luxe' | 'midnight'

function getCurrentTheme(): Theme {
  return document.documentElement.dataset.theme === 'midnight' ||
    document.documentElement.dataset.theme === 'dark'
    ? 'midnight'
    : 'luxe'
}

export function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>(getCurrentTheme)

  function toggleTheme() {
    const nextTheme = theme === 'midnight' ? 'luxe' : 'midnight'

    document.documentElement.dataset.theme = nextTheme
    localStorage.setItem('crm-theme', nextTheme)
    setTheme(nextTheme)
  }

  const isLuxe = theme === 'luxe'

  return (
    <button
      type="button"
      onClick={toggleTheme}
      className={cn(
        'inline-flex h-10 items-center gap-2 rounded-xl border px-3 text-sm font-semibold transition',
        'border-[var(--crm-border)] bg-[var(--crm-surface-glass)] text-[var(--crm-text)]',
        'hover:border-violet-300/70 hover:shadow-[0_14px_32px_rgba(109,93,251,0.14)]',
      )}
      aria-label="Toggle theme preset"
    >
      {isLuxe ? <Sparkles size={17} /> : <Moon size={17} />}
      {isLuxe ? 'Luxe' : 'Midnight'}
    </button>
  )
}
