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
        'crm-primary-action inline-flex h-11 items-center justify-center gap-2 rounded-2xl px-4 text-sm font-semibold transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:hover:translate-y-0',
        className,
      )}
      {...props}
    >
      <Icon size={17} />
      {children}
    </button>
  )
}
