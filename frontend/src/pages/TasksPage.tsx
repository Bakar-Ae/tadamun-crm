import { useEffect, useState, type FormEvent } from 'react'
import { motion, type Variants } from 'framer-motion'
import { Activity, AlertTriangle, CalendarClock, CheckCircle2, ClipboardList, UserRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { EmptyState, GlassCard, PageShell, SearchPanel, StatTile } from '../components/ui'
import { getTasks, type TaskResponse } from '../services/taskService'
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

function statusBadgeClass(status: string) {
  if (status === 'COMPLETED') {
    return 'border-emerald-400/30 bg-emerald-400/10 text-[var(--crm-success-text)]'
  }

  if (status === 'IN_PROGRESS') {
    return 'border-blue-400/30 bg-blue-500/10 text-[var(--crm-primary)]'
  }

  if (status === 'CANCELLED') {
    return 'border-slate-400/20 bg-slate-400/10 text-[var(--crm-text-muted)]'
  }

  return 'border-amber-400/30 bg-amber-400/10 text-[var(--crm-warning-text)]'
}

function priorityBadgeClass(priority: string) {
  if (priority === 'URGENT') {
    return 'border-red-400/30 bg-red-400/10 text-[var(--crm-danger-text)]'
  }

  if (priority === 'HIGH') {
    return 'border-orange-400/30 bg-orange-400/10 text-orange-500'
  }

  if (priority === 'MEDIUM') {
    return 'border-blue-400/30 bg-blue-500/10 text-[var(--crm-primary)]'
  }

  return 'border-slate-400/20 bg-slate-400/10 text-[var(--crm-text-muted)]'
}

function formatDate(value: string | null) {
  if (!value) {
    return '-'
  }

  return new Date(value).toLocaleString()
}

export function TasksPage() {
  const [tasks, setTasks] = useState<PageResponse<TaskResponse> | null>(null)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  function loadTasks(search: string) {
    setLoading(true)
    setError('')

    getTasks(0, 10, search)
      .then(setTasks)
      .catch(() => setError('Could not load tasks. Please try again.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    let ignore = false

    getTasks(0, 10, '')
      .then((data) => {
        if (!ignore) {
          setTasks(data)
        }
      })
      .catch(() => {
        if (!ignore) {
          setError('Could not load tasks. Please try again.')
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

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    loadTasks(keyword)
  }

  const visibleTasks = tasks?.content ?? []
  const openTasks = visibleTasks.filter((task) => task.status === 'OPEN').length
  const completedTasks = visibleTasks.filter((task) => task.status === 'COMPLETED').length
  const urgentTasks = visibleTasks.filter((task) => task.priority === 'URGENT').length

  return (
    <AppLayout>
      <PageShell
        title="Tasks"
        description="Track work, owners, due dates, and priority."
      >
        <motion.section
          className="grid gap-4 sm:grid-cols-3"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <StatTile label="Open" value={openTasks} icon={Activity} tone="amber" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Done" value={completedTasks} icon={CheckCircle2} tone="green" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Urgent" value={urgentTasks} icon={AlertTriangle} tone="red" />
          </motion.div>
        </motion.section>

        <SearchPanel
          value={keyword}
          onChange={setKeyword}
          onSubmit={handleSearch}
          placeholder="Search tasks by title, customer, lead, or assignee"
        />

        {error && (
          <div className="rounded-2xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
            {error}
          </div>
        )}

        <GlassCard className="overflow-hidden p-0">
          <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Task Board</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Showing {visibleTasks.length} of {tasks?.totalElements ?? 0} tasks
              </p>
            </div>

            <div className="rounded-xl bg-[var(--crm-soft-gradient)] p-3 text-[var(--crm-primary)] ring-1 ring-violet-300/25">
              <ClipboardList size={22} />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px] border-collapse text-left text-sm">
              <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                <tr>
                  <th className="px-5 py-3 font-semibold">Task</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3 font-semibold">Priority</th>
                  <th className="px-5 py-3 font-semibold">Assigned To</th>
                  <th className="px-5 py-3 font-semibold">Related Record</th>
                  <th className="px-5 py-3 font-semibold">Due Date</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-[var(--crm-text-muted)]" colSpan={6}>
                      Loading tasks...
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleTasks.map((task) => (
                    <tr key={task.id} className="transition hover:bg-violet-500/5">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)] ring-1 ring-violet-300/25">
                            <ClipboardList size={18} />
                          </div>
                          <div>
                            <p className="font-semibold text-[var(--crm-text)]">{task.title}</p>
                            <p className="text-xs text-[var(--crm-text-muted)]">ID #{task.id}</p>
                          </div>
                        </div>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ring-1 ${statusBadgeClass(
                            task.status,
                          )}`}
                        >
                          {task.status}
                        </span>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ring-1 ${priorityBadgeClass(
                            task.priority,
                          )}`}
                        >
                          {task.priority}
                        </span>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <UserRound size={16} />
                          {task.assignedToUserName ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        {task.customerName ?? task.leadName ?? '-'}
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <CalendarClock size={16} />
                          {formatDate(task.dueDate)}
                        </div>
                      </td>
                    </tr>
                  ))}

                {!loading && visibleTasks.length === 0 && (
                  <EmptyState
                    icon={ClipboardList}
                    title="No tasks found"
                    message="Try another title, customer, lead, or assignee."
                    colSpan={6}
                  />
                )}
              </tbody>
            </table>
          </div>
        </GlassCard>
      </PageShell>
    </AppLayout>
  )
}
