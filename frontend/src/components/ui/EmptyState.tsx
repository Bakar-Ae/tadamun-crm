import type { LucideIcon } from 'lucide-react'

type EmptyStateProps = {
  icon: LucideIcon
  title: string
  message: string
  colSpan?: number
}

export function EmptyState({ icon: Icon, title, message, colSpan }: EmptyStateProps) {
  const content = (
    <div className="flex flex-col items-center justify-center px-5 py-10 text-center">
      <div className="grid h-12 w-12 place-items-center rounded-2xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)] ring-1 ring-violet-300/30">
        <Icon size={22} />
      </div>
      <p className="mt-3 font-semibold text-[var(--crm-text)]">{title}</p>
      <p className="mt-1 max-w-md text-sm leading-6 text-[var(--crm-text-muted)]">{message}</p>
    </div>
  )

  if (colSpan) {
    return (
      <tr>
        <td colSpan={colSpan}>{content}</td>
      </tr>
    )
  }

  return content
}
