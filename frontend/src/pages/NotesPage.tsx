import { useEffect, useState, type FormEvent } from 'react'
import { motion, type Variants } from 'framer-motion'
import { FileText, NotebookText, Search, UserRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { GlassCard, PageShell, StatTile } from '../components/ui'
import { getCustomerNotes, getLeadNotes, type NoteResponse } from '../services/noteService'
import type { PageResponse } from '../services/userService'

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
  const [targetId, setTargetId] = useState('1')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  function loadNotes(type = targetType, rawId = targetId) {
    const id = Number(rawId)

    if (!id) {
      setError('Please enter a valid record ID.')
      setNotes(null)
      setLoading(false)
      return
    }

    setLoading(true)
    setError('')

    const request = type === 'customer' ? getCustomerNotes(id) : getLeadNotes(id)

    request
      .then(setNotes)
      .catch(() => setError('Could not load notes. Please try again.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    let ignore = false

    getCustomerNotes(1)
      .then((data) => {
        if (!ignore) {
          setNotes(data)
        }
      })
      .catch(() => {
        if (!ignore) {
          setError('Could not load notes. Please try again.')
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false)
        }
      })

    return () => {
      ignore = true
    }
  }, [])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    loadNotes()
  }

  const visibleNotes = notes?.content ?? []
  const authors = new Set(visibleNotes.map((note) => note.createdByUserId)).size

  return (
    <AppLayout>
      <PageShell
        title="Notes"
        description="Review activity notes connected to customers and leads."
      >
        <motion.section
          className="grid gap-4 sm:grid-cols-3"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <StatTile label="Loaded Notes" value={visibleNotes.length} icon={NotebookText} tone="blue" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Authors" value={authors} icon={UserRound} tone="green" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Target" value={targetType === 'customer' ? 'Customer' : 'Lead'} icon={Search} tone="amber" />
          </motion.div>
        </motion.section>

        <GlassCard>
          <form onSubmit={handleSubmit}>
            <div className="flex flex-col gap-3 md:flex-row">
              <select
                value={targetType}
                onChange={(event) => setTargetType(event.target.value as 'customer' | 'lead')}
                className="crm-focus h-11 rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)] transition focus:border-cyan-400"
              >
                <option value="customer">Customer</option>
                <option value="lead">Lead</option>
              </select>

              <div className="relative flex-1">
                <Search
                  size={18}
                  className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--crm-text-muted)]"
                />
                <input
                  value={targetId}
                  onChange={(event) => setTargetId(event.target.value)}
                  placeholder="Record ID"
                  className="crm-focus h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] pl-10 pr-3 text-sm text-[var(--crm-text)] transition placeholder:text-[var(--crm-text-muted)] focus:border-cyan-400"
                />
              </div>

              <button className="h-11 rounded-xl bg-cyan-600 px-5 text-sm font-semibold text-white shadow-sm shadow-cyan-900/20 transition hover:-translate-y-0.5 hover:bg-cyan-700">
                Load Notes
              </button>
            </div>
          </form>
        </GlassCard>

        {error && (
          <div className="rounded-2xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
            {error}
          </div>
        )}

        <GlassCard>
          <div className="mb-5 flex items-center justify-between gap-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Note Timeline</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Latest notes for the selected record
              </p>
            </div>

            <div className="rounded-xl bg-cyan-400/10 p-3 text-[var(--crm-accent-text)] ring-1 ring-cyan-300/20">
              <NotebookText size={22} />
            </div>
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
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-cyan-400/10 text-[var(--crm-accent-text)] ring-1 ring-cyan-300/20">
                      <FileText size={18} />
                    </div>

                    <div className="min-w-0 flex-1">
                      <p className="text-sm leading-6 text-[var(--crm-text)]">{note.content}</p>
                      <p className="mt-3 text-xs text-[var(--crm-text-muted)]">
                        By {note.createdByUserName} - {new Date(note.createdAt).toLocaleString()}
                      </p>
                    </div>
                  </div>
                </motion.article>
              ))}

            {!loading && visibleNotes.length === 0 && (
              <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-8 text-center text-sm text-[var(--crm-text-muted)]">
                No notes found
              </div>
            )}
          </div>
        </GlassCard>
      </PageShell>
    </AppLayout>
  )
}
