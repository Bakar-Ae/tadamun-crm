import { cn } from '../../lib/cn'

type StatusBadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'neutral'

type StatusBadgeProps = {
  children: string
  variant?: StatusBadgeVariant
  className?: string
}

const variants: Record<StatusBadgeVariant, string> = {
  success: 'border-emerald-400/30 bg-emerald-400/10 text-[var(--crm-success-text)]',
  warning: 'border-amber-400/30 bg-amber-400/10 text-[var(--crm-warning-text)]',
  danger: 'border-red-400/30 bg-red-400/10 text-[var(--crm-danger-text)]',
  info: 'border-violet-400/30 bg-violet-500/10 text-[var(--crm-primary)]',
  neutral: 'border-slate-400/20 bg-slate-400/10 text-[var(--crm-text-muted)]',
}

export function StatusBadge({ children, variant = 'neutral', className }: StatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold',
        variants[variant],
        className,
      )}
    >
      {children}
    </span>
  )
}
