import type { ButtonHTMLAttributes } from 'react'
import type { LucideIcon } from 'lucide-react'
import { cn } from '../../lib/cn'

type PageActionButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  icon: LucideIcon
}

export function PageActionButton({
  icon: Icon,
  children,
  className,
  ...props
}: PageActionButtonProps) {
  return (
    <button
      type="button"
      className={cn(
        'inline-flex h-11 items-center justify-center gap-2 rounded-2xl bg-[var(--crm-brand-gradient)] px-4 text-sm font-semibold text-white shadow-[0_14px_34px_rgba(109,93,251,0.2)] transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0',
        className,
      )}
      {...props}
    >
      <Icon size={17} />
      {children}
    </button>
  )
}
