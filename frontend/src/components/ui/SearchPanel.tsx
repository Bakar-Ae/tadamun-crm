import type { FormEvent, ReactNode } from 'react'
import { Search } from 'lucide-react'
import { GlassCard } from './GlassCard'

type SearchPanelProps = {
  value: string
  onChange: (value: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  placeholder: string
  submitLabel?: string
  children?: ReactNode
}

export function SearchPanel({
  value,
  onChange,
  onSubmit,
  placeholder,
  submitLabel = 'Search',
  children,
}: SearchPanelProps) {
  return (
    <GlassCard>
      <form onSubmit={onSubmit} role="search">
        <div className="flex flex-col gap-3 md:flex-row">
          {children}

          <div className="relative flex-1">
            <Search
              size={18}
              className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--crm-text-muted)]"
            />
            <input
              value={value}
              onChange={(event) => onChange(event.target.value)}
              placeholder={placeholder}
              aria-label={placeholder}
              className="crm-focus h-11 w-full rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] pl-10 pr-3 text-sm text-[var(--crm-text)] shadow-sm transition placeholder:text-[var(--crm-text-muted)] focus:border-[var(--crm-primary)]"
            />
          </div>

          <button className="h-11 rounded-2xl bg-[var(--crm-brand-gradient)] px-5 text-sm font-semibold text-white shadow-[0_14px_34px_rgba(109,93,251,0.2)] transition hover:-translate-y-0.5 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60">
            {submitLabel}
          </button>
        </div>
      </form>
    </GlassCard>
  )
}
