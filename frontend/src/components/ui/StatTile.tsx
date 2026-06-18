import type { LucideIcon } from 'lucide-react'
import { cn } from '../../lib/cn'

type StatTileProps = {
  label: string
  value: string | number
  icon: LucideIcon
  tone?: 'blue' | 'green' | 'amber' | 'red' | 'slate'
}

const tones = {
  blue: 'bg-cyan-400/10 text-[var(--crm-accent-text)] ring-cyan-300/20',
  green: 'bg-emerald-400/10 text-[var(--crm-success-text)] ring-emerald-300/20',
  amber: 'bg-amber-400/10 text-[var(--crm-warning-text)] ring-amber-300/20',
  red: 'bg-red-400/10 text-[var(--crm-danger-text)] ring-red-300/20',
  slate: 'bg-slate-400/10 text-[var(--crm-text-muted)] ring-slate-300/20',
}

export function StatTile({ label, value, icon: Icon, tone = 'blue' }: StatTileProps) {
  return (
    <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-[var(--crm-text-muted)]">
            {label}
          </p>
          <p className="mt-2 text-2xl font-semibold text-[var(--crm-text)]">{value}</p>
        </div>

        <div className={cn('rounded-xl p-2.5 ring-1', tones[tone])}>
          <Icon size={18} />
        </div>
      </div>
    </div>
  )
}