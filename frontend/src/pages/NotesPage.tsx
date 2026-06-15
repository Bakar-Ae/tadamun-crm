import { useEffect, useState } from 'react'
import { FileText, NotebookText, Search } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { getCustomerNotes, getLeadNotes, type NoteResponse } from '../services/noteService'
import type { PageResponse } from '../services/userService'

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
        if (!ignore) setNotes(data)
      })
      .catch(() => {
        if (!ignore) setError('Could not load notes. Please try again.')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [])

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    loadNotes()
  }

  const visibleNotes = notes?.content ?? []

  return (
    <AppLayout>
      <div className="space-y-6">
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="bg-slate-950 px-6 py-6 text-white">
            <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center">
              <div>
                <p className="text-sm font-medium text-blue-300">Activity Notes</p>
                <h2 className="mt-2 text-3xl font-semibold">Notes</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-300">
                  Review notes connected to customers and leads.
                </p>
              </div>

              <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                <p className="text-xs text-slate-400">Loaded Notes</p>
                <p className="mt-1 text-xl font-semibold">{visibleNotes.length}</p>
              </div>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="border-t border-slate-200 p-5">
            <div className="flex flex-col gap-3 md:flex-row">
              <select
                value={targetType}
                onChange={(event) => setTargetType(event.target.value as 'customer' | 'lead')}
                className="h-11 rounded-lg border border-slate-300 bg-white px-3 text-sm outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
              >
                <option value="customer">Customer</option>
                <option value="lead">Lead</option>
              </select>

              <div className="relative flex-1">
                <Search size={18} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  value={targetId}
                  onChange={(event) => setTargetId(event.target.value)}
                  placeholder="Record ID"
                  className="h-11 w-full rounded-lg border border-slate-300 bg-white pl-10 pr-3 text-sm outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                />
              </div>

              <button className="h-11 rounded-lg bg-blue-600 px-5 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700">
                Load Notes
              </button>
            </div>
          </form>
        </section>

        {error && (
          <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
            {error}
          </div>
        )}

        <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-slate-950">Note Timeline</h3>
              <p className="text-sm text-slate-500">Latest notes for the selected record</p>
            </div>

            <div className="rounded-lg bg-blue-50 p-3 text-blue-700">
              <NotebookText size={22} />
            </div>
          </div>

          <div className="space-y-3">
            {loading && <p className="py-8 text-center text-sm text-slate-500">Loading notes...</p>}

            {!loading &&
              visibleNotes.map((note) => (
                <article key={note.id} className="rounded-lg border border-slate-200 bg-slate-50 p-4">
                  <div className="flex gap-3">
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-blue-600 text-white">
                      <FileText size={18} />
                    </div>

                    <div className="min-w-0 flex-1">
                      <p className="text-sm leading-6 text-slate-800">{note.content}</p>
                      <p className="mt-3 text-xs text-slate-500">
                        By {note.createdByUserName} - {new Date(note.createdAt).toLocaleString()}
                      </p>
                    </div>
                  </div>
                </article>
              ))}

            {!loading && visibleNotes.length === 0 && (
              <div className="rounded-lg border border-slate-200 bg-slate-50 p-8 text-center text-sm text-slate-500">
                No notes found
              </div>
            )}
          </div>
        </section>
      </div>
    </AppLayout>
  )
}