import { ClipboardList, FileText, ShieldCheck } from 'lucide-react'
import { formatDateTime } from '../../lib/formatters'

export type ActivityTimelineItem = {
  id: string | number
  type: 'note' | 'task' | 'audit'
  title: string
  description?: string | null
  actor?: string | null
  createdAt: string | null
}

type ActivityTimelineProps = {
  items: ActivityTimelineItem[]
  loading?: boolean
  emptyTitle?: string
  emptyMessage?: string
}

function getIcon(type: ActivityTimelineItem['type']) {
  if (type === 'task') return ClipboardList
  if (type === 'audit') return ShieldCheck
  return FileText
}

export function ActivityTimeline({
  items,
  loading = false,
  emptyTitle = 'No activity yet',
  emptyMessage = 'Notes, tasks, and record updates will appear here.',
}: ActivityTimelineProps) {
  if (loading) {
    return (
      <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4 text-sm text-[var(--crm-text-muted)]">
        Loading activity...
      </div>
    )
  }

  if (items.length === 0) {
    return (
      <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-5">
        <p className="font-semibold text-[var(--crm-text)]">{emptyTitle}</p>
        <p className="mt-1 text-sm text-[var(--crm-text-muted)]">{emptyMessage}</p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {items.map((item) => {
        const Icon = getIcon(item.type)

        return (
          <article
            key={`${item.type}-${item.id}`}
            className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4"
          >
            <div className="flex gap-3">
              <div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)] ring-1 ring-violet-300/25">
                <Icon size={18} />
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="font-semibold text-[var(--crm-text)]">{item.title}</p>
                  <p className="text-xs text-[var(--crm-text-muted)]">
                    {formatDateTime(item.createdAt)}
                  </p>
                </div>

                {item.description && (
                  <p className="mt-2 text-sm leading-6 text-[var(--crm-text-muted)]">
                    {item.description}
                  </p>
                )}

                {item.actor && (
                  <p className="mt-3 text-xs font-medium text-[var(--crm-text-muted)]">
                    {item.actor}
                  </p>
                )}
              </div>
            </div>
          </article>
        )
      })}
    </div>
  )
}