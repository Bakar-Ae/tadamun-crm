import type { LucideIcon } from 'lucide-react'
import { ArrowUpRight } from 'lucide-react'
import { GlassCard } from './GlassCard'
import { cn } from '../../lib/cn'

type MetricCardProps = {
  label: string
  value: string | number
  icon: LucideIcon
  tone?: 'blue' | 'green' | 'amber' | 'slate'
  trend?: string
}

const tones = {
  blue: 'bg-cyan-400/10 text-cyan-200 ring-cyan-300/20',
  green: 'bg-emerald-400/10 text-emerald-200 ring-emerald-300/20',
  amber: 'bg-amber-400/10 text-amber-200 ring-amber-300/20',
  slate: 'bg-slate-400/10 text-slate-200 ring-slate-300/20',
}

export function MetricCard({ label, value, icon: Icon, tone = 'blue', trend }: MetricCardProps) {
  return (
    <GlassCard>
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-[var(--crm-text-muted)]">{label}</p>
          <p className="mt-3 text-3xl font-semibold tracking-normal text-[var(--crm-text)]">{value}</p>

          {trend && (
            <p className="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-emerald-300">
              <ArrowUpRight size={14} />
              {trend}
            </p>
          )}
        </div>

        <div className={cn('rounded-2xl p-3 ring-1', tones[tone])}>
          <Icon size={22} />
        </div>
      </div>
    </GlassCard>
  )
}