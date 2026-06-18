import { cn } from '../../lib/cn'

type StatusBadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'neutral'

type StatusBadgeProps = {
  children: string
  variant?: StatusBadgeVariant
  className?: string
}

const variants: Record<StatusBadgeVariant, string> = {
  success: 'border-emerald-400/30 bg-emerald-400/10 text-emerald-200',
  warning: 'border-amber-400/30 bg-amber-400/10 text-amber-200',
  danger: 'border-red-400/30 bg-red-400/10 text-red-200',
  info: 'border-cyan-400/30 bg-cyan-400/10 text-cyan-200',
  neutral: 'border-slate-400/20 bg-slate-400/10 text-slate-200',
}

export function StatusBadge({ children, variant = 'neutral', className }: StatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold uppercase tracking-wide',
        variants[variant],
        className,
      )}
    >
      {children}
    </span>
  )
}