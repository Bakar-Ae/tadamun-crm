import { useEffect, useState, type FormEvent } from 'react'
import { motion, type Variants } from 'framer-motion'
import { FileText, NotebookText, Plus, UserRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import {
  EmptyState,
  GlassCard,
  PageActionButton,
  PageShell,
  SearchPanel,
  StatTile,
  StatusBadge,
} from '../components/ui'
import { getCustomerNotes, getLeadNotes, type NoteResponse } from '../services/noteService'
import type { PageResponse } from '../services/userService'
import { formatDateTime } from '../lib/formatters'
import { openQuickCreate } from '../lib/quickCreate'

const containerAnimation: Variants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.08,
    },
  },
}

const cardAnimation: Variants = {
  hidden: { opacity: 0, y: 16 },
  show: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.3,
      ease: 'easeOut',
    },
  },
}

export function NotesPage() {
  const [notes, setNotes] = useState<PageResponse<NoteResponse> | null>(null)
  const [targetType, setTargetType] = useState<'customer' | 'lead'>('customer')
  const [targetId, setTargetId] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [hasLoaded, setHasLoaded] = useState(false)

  function loadNotes(type = targetType, rawId = targetId) {
    const id = Number(rawId)

    if (!Number.isInteger(id) || id <= 0) {
      setError('Enter a valid customer or lead ID.')
      setNotes(null)
      setHasLoaded(false)
      return
    }

    setLoading(true)
    setError('')
    setHasLoaded(true)

    const request = type === 'customer' ? getCustomerNotes(id) : getLeadNotes(id)

    request
      .then(setNotes)
      .catch(() => setError('Notes could not be loaded. Please try again.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    function refreshAfterCreate() {
      if (targetId.trim()) {
        loadNotes()
      }
    }

    window.addEventListener('crm-data-changed', refreshAfterCreate)
    return () => window.removeEventListener('crm-data-changed', refreshAfterCreate)
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    loadNotes()
  }

  const visibleNotes = notes?.content ?? []
  const authors = new Set(visibleNotes.map((note) => note.createdByUserId)).size
  const selectedRecordLabel = targetId.trim()
    ? `${targetType === 'customer' ? 'Customer' : 'Lead'} ${targetId}`
    : 'No record selected'

  return (
    <AppLayout>
      <PageShell
        title="Notes"
        description="Review conversation notes attached to customer and lead records."
        action={
          <PageActionButton icon={Plus} onClick={() => openQuickCreate('note')}>
            Add note
          </PageActionButton>
        }
      >
        <motion.section
          className="grid gap-4 sm:grid-cols-3"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <StatTile label="Notes" value={visibleNotes.length} icon={NotebookText} tone="blue" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Contributors" value={authors} icon={UserRound} tone="green" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
              <p className="text-xs font-medium uppercase tracking-wide text-[var(--crm-text-muted)]">
                Selected customer or lead
              </p>
              <p className="mt-2 text-lg font-semibold text-[var(--crm-text)]">{selectedRecordLabel}</p>
            </div>
          </motion.div>
        </motion.section>

        <SearchPanel
          value={targetId}
          onChange={setTargetId}
          onSubmit={handleSubmit}
          placeholder="Enter customer or lead number"
          submitLabel="Load notes"
        >
          <select
            value={targetType}
            onChange={(event) => setTargetType(event.target.value as 'customer' | 'lead')}
            aria-label="Note record type"
            className="crm-focus h-11 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)] shadow-sm transition focus:border-[var(--crm-primary)]"
          >
            <option value="customer">Customer</option>
            <option value="lead">Lead</option>
          </select>
        </SearchPanel>

        {error && (
          <div className="rounded-2xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
            {error}
          </div>
        )}

        <GlassCard>
          <div className="mb-5 flex items-center justify-between gap-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Notes list</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Load a customer or lead to review its note history.
              </p>
            </div>
            <StatusBadge variant="info">{selectedRecordLabel}</StatusBadge>
          </div>

          <div className="space-y-3">
            {loading && (
              <p className="py-8 text-center text-sm text-[var(--crm-text-muted)]">Loading notes...</p>
            )}

            {!loading &&
              visibleNotes.map((note) => (
                <motion.article
                  key={note.id}
                  className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4"
                  variants={cardAnimation}
                  initial="hidden"
                  animate="show"
                >
                  <div className="flex gap-3">
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)] ring-1 ring-violet-300/25">
                      <FileText size={18} />
                    </div>

                    <div className="min-w-0 flex-1">
                      <p className="text-sm leading-6 text-[var(--crm-text)]">{note.content}</p>
                      <p className="mt-3 text-xs text-[var(--crm-text-muted)]">
                        {note.createdByUserName} · {formatDateTime(note.createdAt)}
                      </p>
                    </div>
                  </div>
                </motion.article>
              ))}

            {!loading && visibleNotes.length === 0 && (
              <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)]">
                <EmptyState
                  icon={NotebookText}
                  title={hasLoaded ? 'No notes yet' : 'Choose a customer or lead to view notes'}
                  message={
                    hasLoaded
                      ? 'Add a note when there is a useful customer conversation to remember.'
                      : 'Select customer or lead, enter its ID, then load the note history.'
                  }
                  action={
                    <PageActionButton icon={Plus} onClick={() => openQuickCreate('note')}>
                      Add note
                    </PageActionButton>
                  }
                />
              </div>
            )}
          </div>
        </GlassCard>
      </PageShell>
    </AppLayout>
  )
}
