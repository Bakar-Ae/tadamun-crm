import { useCallback, useRef, useState } from 'react'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import listPlugin from '@fullcalendar/list'
import type {
  EventClickArg,
  EventInput,
  EventSourceFuncArg,
} from '@fullcalendar/core'
import { Plus } from 'lucide-react'
import { useNavigate } from 'react-router'
import { AppLayout } from '../layouts/AppLayout'
import {
  ErrorState,
  GlassCard,
  PageActionButton,
  PageShell,
} from '../components/ui'
import {
  getCalendarTasks,
  type CalendarTaskResponse,
} from '../services/taskService'
import { getLoadErrorMessage } from '../lib/errors'
import { openQuickCreate } from '../lib/quickCreate'

function toCalendarEvent(task: CalendarTaskResponse): EventInput {
  return {
    id: String(task.id),
    title: task.title,
    start: task.dueDate,
    classNames: [
      'crm-calendar-event',
      `crm-calendar-event-${task.priority.toLowerCase()}`,
    ],
    extendedProps: { task },
  }
}

export function CalendarPage() {
  const navigate = useNavigate()
  const calendarRef = useRef<FullCalendar | null>(null)
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [visibleCount, setVisibleCount] = useState<number | null>(null)

  const loadEvents = useCallback(
    async (info: EventSourceFuncArg): Promise<EventInput[]> => {
      setLoadError('')

      try {
        const response = await getCalendarTasks({
          from: info.startStr,
          to: info.endStr,
          size: 500,
        })

        setVisibleCount(response.totalElements)
        return response.content.map(toCalendarEvent)
      } catch (error) {
        setVisibleCount(null)
        setLoadError(getLoadErrorMessage('calendar'))
        throw error
      }
    },
    [],
  )

  function retryLoad() {
    setLoadError('')
    calendarRef.current?.getApi().refetchEvents()
  }

  function handleEventClick(info: EventClickArg) {
    navigate(`/tasks?taskId=${info.event.id}`)
  }

  return (
    <AppLayout>
      <PageShell
        title="Calendar"
        description="Review tasks and follow-ups by due date."
        action={
          <PageActionButton
            icon={Plus}
            onClick={() => openQuickCreate('task')}
          >
            New task
          </PageActionButton>
        }
      >
        {loadError && (
          <ErrorState message={loadError} onRetry={retryLoad} />
        )}

        <GlassCard className="relative overflow-hidden p-4 sm:p-5">
          {loading && (
            <div
              className="absolute right-5 top-5 z-10 rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 py-2 text-xs font-semibold text-[var(--crm-text-muted)] shadow-sm"
              role="status"
            >
              Loading schedule...
            </div>
          )}

          {!loading && !loadError && visibleCount === 0 && (
            <p className="mb-4 rounded-xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] px-4 py-3 text-sm text-[var(--crm-text-muted)]">
              No tasks are due in this period.
            </p>
          )}

          <div className="crm-calendar" aria-busy={loading}>
            <FullCalendar
              ref={calendarRef}
              plugins={[dayGridPlugin, timeGridPlugin, listPlugin]}
              initialView="dayGridMonth"
              headerToolbar={{
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,listMonth',
              }}
              buttonText={{
                today: 'Today',
                month: 'Month',
                week: 'Week',
                list: 'Agenda',
              }}
              events={loadEvents}
              eventClick={handleEventClick}
              loading={setLoading}
              timeZone="local"
              nowIndicator
              dayMaxEvents={3}
              height="auto"
            />
          </div>
        </GlassCard>
      </PageShell>
    </AppLayout>
  )
}