import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { motion, type Variants } from 'framer-motion'
import toast from 'react-hot-toast'
import {
  Activity,
  AlertTriangle,
  CalendarClock,
  CheckCircle2,
  ClipboardList,
  Plus,
  UserRound,
} from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import {
  DetailDrawer,
  EmptyState,
  GlassCard,
  PageActionButton,
  PageShell,
  SearchPanel,
  StatTile,
  StatusBadge,
  LoadingState,
  ErrorState,
  PaginationBar,
} from '../components/ui'
import { getTasks, updateTask, type TaskResponse } from '../services/taskService'
import type { PageResponse } from '../services/userService'
import {
  formatDateTime,
  formatStatus,
  getEmptyMessage,
  priorityVariant,
  statusVariant,
} from '../lib/formatters'
import { getLoadErrorMessage, getSaveErrorMessage } from '../lib/errors'
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

function isOverdue(task: TaskResponse) {
  if (!task.dueDate || task.status === 'COMPLETED' || task.status === 'CANCELLED') {
    return false
  }

  return new Date(task.dueDate).getTime() < Date.now()
}

export function TasksPage() {
  const [tasks, setTasks] = useState<PageResponse<TaskResponse> | null>(null)
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedTask, setSelectedTask] = useState<TaskResponse | null>(null)
  const [editingTask, setEditingTask] = useState(false)
  const [editTaskForm, setEditTaskForm] = useState({
    title: '',
    description: '',
    status: 'OPEN' as TaskResponse['status'],
    priority: 'MEDIUM' as TaskResponse['priority'],
    dueDate: '',
  })
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)

  const loadTasks = useCallback((search: string, pageNumber = page, size = pageSize) => {
    setLoading(true)
    setError('')

    getTasks(pageNumber, size, search)
      .then(setTasks)
      .catch(() => setError(getLoadErrorMessage('tasks')))
      .finally(() => setLoading(false))
  }, [page, pageSize])

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
          setError(getLoadErrorMessage('tasks'))
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

  useEffect(() => {
    function refreshAfterCreate() {
      loadTasks(keyword)
    }

    window.addEventListener('crm-data-changed', refreshAfterCreate)
    return () => window.removeEventListener('crm-data-changed', refreshAfterCreate)
  }, [keyword, loadTasks])

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPage(0)
    loadTasks(keyword,0)
  }

  async function handleComplete(task: TaskResponse) {
    setActionLoadingId(task.id)

    try {
      await updateTask(task.id, {
        title: task.title,
        description: task.description,
        status: 'COMPLETED',
        priority: task.priority,
        dueDate: task.dueDate,
        assignedToUserId: task.assignedToUserId,
        customerId: task.customerId,
        leadId: task.leadId,
      })
      toast.success('Task completed')
      loadTasks(keyword)
    } catch {
      toast.error(getSaveErrorMessage('task'))
    } finally {
      setActionLoadingId(null)
    }
  }

  function startEditingTask(task: TaskResponse) {
  setSelectedTask(task)
  setEditTaskForm({
    title: task.title,
    description: task.description ?? '',
    status: task.status,
    priority: task.priority,
    dueDate: task.dueDate ? task.dueDate.slice(0, 16) : '',
  })
  setEditingTask(true)
}

function cancelEditingTask() {
  setEditingTask(false)

  if (selectedTask) {
    setEditTaskForm({
      title: selectedTask.title,
      description: selectedTask.description ?? '',
      status: selectedTask.status,
      priority: selectedTask.priority,
      dueDate: selectedTask.dueDate ? selectedTask.dueDate.slice(0, 16) : '',
    })
  }
}

async function saveTaskEdit() {
  if (!selectedTask) return

  setActionLoadingId(selectedTask.id)

  try {
    const updatedTask = await updateTask(selectedTask.id, {
      title: editTaskForm.title.trim(),
      description: editTaskForm.description.trim() || null,
      status: editTaskForm.status,
      priority: editTaskForm.priority,
      dueDate: editTaskForm.dueDate || null,
      assignedToUserId: selectedTask.assignedToUserId,
      customerId: selectedTask.customerId,
      leadId: selectedTask.leadId,
    })

    setSelectedTask(updatedTask)
    setEditingTask(false)
    toast.success('Task updated')
    loadTasks(keyword)
  } catch {
    toast.error(getSaveErrorMessage('task'))
  } finally {
    setActionLoadingId(null)
  }
}
  function goToPreviousPage() {
    const previousPage = Math.max(page - 1, 0)
    setPage(previousPage)
    loadTasks(keyword, previousPage)
  }

  function goToNextPage() {
    const nextPage = page + 1
    setPage(nextPage)
    loadTasks(keyword, nextPage)
  }

  function handlePageSizeChange(nextPageSize: number) {
    setPageSize(nextPageSize)
    setPage(0)
    loadTasks(keyword, 0, nextPageSize)
  }

  const visibleTasks = tasks?.content ?? []
  const openTasks = visibleTasks.filter((task) => task.status === 'OPEN').length
  const completedTasks = visibleTasks.filter((task) => task.status === 'COMPLETED').length
  const urgentTasks = visibleTasks.filter((task) => task.priority === 'URGENT').length
  const overdueTasks = visibleTasks.filter(isOverdue).length
  const hasSearch = keyword.trim().length > 0

  return (
    <AppLayout>
      <PageShell
        title="Tasks"
        description="Plan follow-ups, assign work, and keep customer activity moving."
        action={
          <PageActionButton icon={Plus} onClick={() => openQuickCreate('task')}>
            New task
          </PageActionButton>
        }
      >
        <motion.section
          className="grid gap-4 sm:grid-cols-4"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <StatTile label="Open" value={openTasks} icon={Activity} tone="amber" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Completed" value={completedTasks} icon={CheckCircle2} tone="green" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Urgent" value={urgentTasks} icon={AlertTriangle} tone="red" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Overdue" value={overdueTasks} icon={CalendarClock} tone="red" />
          </motion.div>
        </motion.section>

        <SearchPanel
          value={keyword}
          onChange={setKeyword}
          onSubmit={handleSearch}
          placeholder="Search tasks by title, customer, lead, or owner"
        />

        {error && <ErrorState message={error} onRetry={() => loadTasks(keyword)} />}

        <GlassCard className="overflow-hidden p-0">
          <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Work list</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Showing {visibleTasks.length} of {tasks?.totalElements ?? 0} tasks
              </p>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[1040px] border-collapse text-left text-sm">
              <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                <tr>
                  <th className="px-5 py-3 font-semibold">Task</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3 font-semibold">Priority</th>
                  <th className="px-5 py-3 font-semibold">Owner</th>
                  <th className="px-5 py-3 font-semibold">Related record</th>
                  <th className="px-5 py-3 font-semibold">Due</th>
                  <th className="sticky right-0 bg-[var(--crm-card-subtle)] px-5 py-3 text-right font-semibold">Action</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td colSpan={7}>
                      <LoadingState message="Loading tasks ... " />

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
                            {task.description && (
                              <p className="mt-1 line-clamp-1 max-w-xs text-xs text-[var(--crm-text-muted)]">
                                {task.description}
                              </p>
                            )}
                          </div>
                        </div>
                      </td>

                      <td className="px-5 py-4">
                        <StatusBadge variant={statusVariant(task.status)}>
                          {formatStatus(task.status)}
                        </StatusBadge>
                      </td>

                      <td className="px-5 py-4">
                        <StatusBadge variant={priorityVariant(task.priority)}>
                          {formatStatus(task.priority)}
                        </StatusBadge>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <UserRound size={16} />
                          {task.assignedToUserName ?? 'Unassigned'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        {task.customerName ?? task.leadName ?? 'No linked record'}
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <CalendarClock size={16} />
                          <span className={isOverdue(task) ? 'font-semibold text-[var(--crm-danger-text)]' : ''}>
                            {formatDateTime(task.dueDate)}
                          </span>
                        </div>
                      </td>

                     <td className="sticky right-0 bg-[var(--crm-card)] px-5 py-4 text-right">
                     <div className="flex justify-end gap-2">
                       <button
                         type="button"
                         onClick={() => {
                          setSelectedTask(task)
                          setEditingTask(false)
                        }}
                         className="inline-flex h-9 items-center justify-center rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-violet-300 hover:bg-violet-500/10 hover:text-[var(--crm-primary)]"
                       >
                         View
                       </button>
                   
                       <button
                         type="button"
                         onClick={() => handleComplete(task)}
                         disabled={task.status === 'COMPLETED' || actionLoadingId === task.id}
                         className="inline-flex h-9 items-center justify-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-emerald-300 hover:bg-emerald-400/10 hover:text-[var(--crm-success-text)] disabled:cursor-not-allowed disabled:opacity-50"
                       >
                         <CheckCircle2 size={14} />
                         {actionLoadingId === task.id ? 'Saving...' : 'Complete'}
                       </button>
                     </div>
                   </td> 
                    </tr>
                  ))}

                {!loading && visibleTasks.length === 0 && (
                  <EmptyState
                    icon={ClipboardList}
                    title={hasSearch ? 'No tasks found' : 'No tasks yet'}
                    message={getEmptyMessage(hasSearch, 'tasks', 'New task')}
                    colSpan={7}
                    action={
                      !hasSearch && (
                        <PageActionButton icon={Plus} onClick={() => openQuickCreate('task')}>
                          New task
                        </PageActionButton>
                      )
                    }
                    
                  />
                )}
              </tbody>
            </table>
          </div>
          {tasks && (
            <PaginationBar
              page={page}
              totalPages={tasks.totalPages}
              totalElements={tasks.totalElements}
              pageSize={pageSize}
              onPrevious={goToPreviousPage}
              onNext={goToNextPage}
              onPageSizeChange={handlePageSizeChange}
              disabled={loading}
            />
            
          )}
        </GlassCard>
        <DetailDrawer
        open={selectedTask !== null}
        title={selectedTask?.title ?? 'Task details'}
        description={selectedTask?.customerName ?? selectedTask?.leadName ?? 'Work item'}
        onClose={() => {
          setSelectedTask(null)
          setEditingTask(false)
        }}
        footer={
          selectedTask && (
            <div className="flex justify-end gap-2">
              {editingTask ? (
                <>
                  <button
                    type="button"
                    onClick={cancelEditingTask}
                    className="inline-flex h-10 items-center justify-center rounded-xl border border-[var(--crm-border)] px-4 text-sm font-semibold text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-text)]"
                  >
                    Cancel
                  </button>
      
                  <button
                    type="button"
                    onClick={saveTaskEdit}
                    disabled={actionLoadingId === selectedTask.id}
                    className="inline-flex h-10 items-center justify-center rounded-xl bg-[var(--crm-primary)] px-4 text-sm font-semibold text-white shadow-lg shadow-violet-500/20 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {actionLoadingId === selectedTask.id ? 'Saving...' : 'Save changes'}
                  </button>
                </>
              ) : (
                <button
                  type="button"
                  onClick={() => startEditingTask(selectedTask)}
                  className="inline-flex h-10 items-center justify-center rounded-xl bg-[var(--crm-primary)] px-4 text-sm font-semibold text-white shadow-lg shadow-violet-500/20 transition hover:brightness-110"
                >
                  Edit task
                </button>
              )}
            </div>
          )
        }
      >
        {selectedTask && (
          <div className="space-y-5">
            <section className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
              <h3 className="font-semibold text-[var(--crm-text)]">Task information</h3>
      
              {editingTask ? (
                <div className="mt-4 grid gap-4 sm:grid-cols-2">
                  <label className="block sm:col-span-2">
                    <span className="text-xs uppercase text-[var(--crm-text-muted)]">Title</span>
                    <input
                      value={editTaskForm.title}
                      onChange={(event) =>
                        setEditTaskForm((current) => ({ ...current, title: event.target.value }))
                      }
                      className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                    />
                  </label>
      
                  <label className="block">
                    <span className="text-xs uppercase text-[var(--crm-text-muted)]">Status</span>
                    <select
                      value={editTaskForm.status}
                      onChange={(event) =>
                        setEditTaskForm((current) => ({
                          ...current,
                          status: event.target.value as TaskResponse['status'],
                        }))
                      }
                      className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                    >
                      <option value="OPEN">Open</option>
                      <option value="IN_PROGRESS">In progress</option>
                      <option value="COMPLETED">Completed</option>
                      <option value="CANCELLED">Cancelled</option>
                    </select>
                  </label>
      
                  <label className="block">
                    <span className="text-xs uppercase text-[var(--crm-text-muted)]">Priority</span>
                    <select
                      value={editTaskForm.priority}
                      onChange={(event) =>
                        setEditTaskForm((current) => ({
                          ...current,
                          priority: event.target.value as TaskResponse['priority'],
                        }))
                      }
                      className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                    >
                      <option value="LOW">Low</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="HIGH">High</option>
                      <option value="URGENT">Urgent</option>
                    </select>
                  </label>
      
                  <label className="block">
                    <span className="text-xs uppercase text-[var(--crm-text-muted)]">Due date</span>
                    <input
                      type="datetime-local"
                      value={editTaskForm.dueDate}
                      onChange={(event) =>
                        setEditTaskForm((current) => ({ ...current, dueDate: event.target.value }))
                      }
                      className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                    />
                  </label>
      
                  <div>
                    <p className="text-xs uppercase text-[var(--crm-text-muted)]">Owner</p>
                    <p className="mt-2 font-medium text-[var(--crm-text)]">
                      {selectedTask.assignedToUserName ?? 'Unassigned'}
                    </p>
                  </div>
      
                  <label className="block sm:col-span-2">
                    <span className="text-xs uppercase text-[var(--crm-text-muted)]">Description</span>
                    <textarea
                      value={editTaskForm.description}
                      onChange={(event) =>
                        setEditTaskForm((current) => ({ ...current, description: event.target.value }))
                      }
                      rows={4}
                      className="crm-focus mt-1 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 py-3 text-sm text-[var(--crm-text)]"
                    />
                  </label>
                </div>
              ) : (
                <dl className="mt-4 grid gap-4 sm:grid-cols-2">
                  <div>
                    <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Title</dt>
                    <dd className="mt-1 font-medium text-[var(--crm-text)]">{selectedTask.title}</dd>
                  </div>
      
                  <div>
                    <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Owner</dt>
                    <dd className="mt-1 font-medium text-[var(--crm-text)]">
                      {selectedTask.assignedToUserName ?? 'Unassigned'}
                    </dd>
                  </div>
      
                  <div>
                    <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Status</dt>
                    <dd className="mt-1">
                      <StatusBadge variant={statusVariant(selectedTask.status)}>
                        {formatStatus(selectedTask.status)}
                      </StatusBadge>
                    </dd>
                  </div>
      
                  <div>
                    <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Priority</dt>
                    <dd className="mt-1">
                      <StatusBadge variant={priorityVariant(selectedTask.priority)}>
                        {formatStatus(selectedTask.priority)}
                      </StatusBadge>
                    </dd>
                  </div>
      
                  <div>
                    <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Related record</dt>
                    <dd className="mt-1 font-medium text-[var(--crm-text)]">
                      {selectedTask.customerName ?? selectedTask.leadName ?? 'No linked record'}
                    </dd>
                  </div>
      
                  <div>
                    <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Due date</dt>
                    <dd className="mt-1 font-medium text-[var(--crm-text)]">
                      {formatDateTime(selectedTask.dueDate)}
                    </dd>
                  </div>
                </dl>
              )}
      
              {!editingTask && selectedTask.description && (
                <div className="mt-5">
                  <p className="text-xs uppercase text-[var(--crm-text-muted)]">Description</p>
                  <p className="mt-2 rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] p-3 text-sm text-[var(--crm-text-muted)]">
                    {selectedTask.description}
                  </p>
                </div>
              )}
            </section>
      
            <section className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
              <h3 className="font-semibold text-[var(--crm-text)]">Record activity</h3>
              <p className="mt-2 text-sm text-[var(--crm-text-muted)]">
                Created {formatDateTime(selectedTask.createdAt)}. Last updated {formatDateTime(selectedTask.updatedAt)}.
              </p>
            </section>
          </div>
        )}
      </DetailDrawer>
      </PageShell>
    </AppLayout>
  )
}
