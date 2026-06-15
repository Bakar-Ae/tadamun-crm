import { useEffect, useState } from 'react'
import { CalendarClock, ClipboardList, Search, UserRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { getTasks, type TaskResponse } from '../services/taskService'
import type { PageResponse } from '../services/userService'

function statusBadgeClass(status: string) {
  if (status === 'COMPLETED') {
    return 'bg-emerald-50 text-emerald-700 ring-emerald-200'
  }

  if (status === 'IN_PROGRESS') {
    return 'bg-blue-50 text-blue-700 ring-blue-200'
  }

  if (status === 'CANCELLED') {
    return 'bg-slate-100 text-slate-600 ring-slate-200'
  }

  return 'bg-amber-50 text-amber-700 ring-amber-200'
}

function priorityBadgeClass(priority: string) {
  if (priority === 'URGENT') {
    return 'bg-red-50 text-red-700 ring-red-200'
  }

  if (priority === 'HIGH') {
    return 'bg-orange-50 text-orange-700 ring-orange-200'
  }

  if (priority === 'MEDIUM') {
    return 'bg-blue-50 text-blue-700 ring-blue-200'
  }

  return 'bg-slate-100 text-slate-600 ring-slate-200'
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

  function handleSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    loadTasks(keyword)
  }

  const visibleTasks = tasks?.content ?? []
  const openTasks = visibleTasks.filter((task) => task.status === 'OPEN').length
  const completedTasks = visibleTasks.filter((task) => task.status === 'COMPLETED').length
  const urgentTasks = visibleTasks.filter((task) => task.priority === 'URGENT').length

  return (
    <AppLayout>
      <div className="space-y-6">
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="bg-slate-950 px-6 py-6 text-white">
            <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center">
              <div>
                <p className="text-sm font-medium text-amber-300">Task Management</p>
                <h2 className="mt-2 text-3xl font-semibold">Tasks</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-300">
                  Track follow-ups, ownership, due dates, task status, and priority.
                </p>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Open</p>
                  <p className="mt-1 text-xl font-semibold">{openTasks}</p>
                </div>
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Done</p>
                  <p className="mt-1 text-xl font-semibold">{completedTasks}</p>
                </div>
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Urgent</p>
                  <p className="mt-1 text-xl font-semibold">{urgentTasks}</p>
                </div>
              </div>
            </div>
          </div>

          <form onSubmit={handleSearch} className="border-t border-slate-200 p-5">
            <div className="flex flex-col gap-3 md:flex-row">
              <div className="relative flex-1">
                <Search
                  size={18}
                  className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                />
                <input
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="Search tasks by title, customer, lead, or assignee"
                  className="h-11 w-full rounded-lg border border-slate-300 bg-white pl-10 pr-3 text-sm outline-none transition focus:border-amber-600 focus:ring-4 focus:ring-amber-100"
                />
              </div>

              <button className="h-11 rounded-lg bg-amber-600 px-5 text-sm font-semibold text-white shadow-sm transition hover:bg-amber-700">
                Search
              </button>
            </div>
          </form>
        </section>

        {error && (
          <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
            {error}
          </div>
        )}

        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
            <div>
              <h3 className="font-semibold text-slate-950">Task Board</h3>
              <p className="text-sm text-slate-500">
                Showing {visibleTasks.length} of {tasks?.totalElements ?? 0} tasks
              </p>
            </div>

            <div className="rounded-lg bg-amber-50 p-3 text-amber-700">
              <ClipboardList size={22} />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px] border-collapse text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-semibold">Task</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3 font-semibold">Priority</th>
                  <th className="px-5 py-3 font-semibold">Assigned To</th>
                  <th className="px-5 py-3 font-semibold">Related Record</th>
                  <th className="px-5 py-3 font-semibold">Due Date</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-slate-500" colSpan={6}>
                      Loading tasks...
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleTasks.map((task) => (
                    <tr key={task.id} className="transition hover:bg-slate-50">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-amber-600 text-white">
                            <ClipboardList size={18} />
                          </div>
                          <div>
                            <p className="font-semibold text-slate-950">{task.title}</p>
                            <p className="text-xs text-slate-500">ID #{task.id}</p>
                          </div>
                        </div>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${statusBadgeClass(
                            task.status,
                          )}`}
                        >
                          {task.status}
                        </span>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${priorityBadgeClass(
                            task.priority,
                          )}`}
                        >
                          {task.priority}
                        </span>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <UserRound size={16} className="text-slate-400" />
                          {task.assignedToUserName ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        {task.customerName ?? task.leadName ?? '-'}
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <CalendarClock size={16} className="text-slate-400" />
                          {task.dueDate ? new Date(task.dueDate).toLocaleString() : '-'}
                        </div>
                      </td>
                    </tr>
                  ))}

                {!loading && visibleTasks.length === 0 && (
                  <tr>
                    <td className="px-5 py-10 text-center text-slate-500" colSpan={6}>
                      No tasks found
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </AppLayout>
  )
}