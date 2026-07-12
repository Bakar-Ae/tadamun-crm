import { useEffect, useState, type CSSProperties, type FormEvent } from 'react'
import {
  Activity,
  ArrowUpRight,
  BarChart3,
  CheckCircle2,
  ClipboardList,
  Download,
  RefreshCw,
  Target,
  UsersRound,
  FileText,
  FileSpreadsheet,
} from 'lucide-react'
import toast from 'react-hot-toast'
import { NavLink } from 'react-router'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { AppLayout } from '../layouts/AppLayout'
import {
  EmptyState,
  ErrorState,
  GlassCard,
  LoadingState,
  MetricCard,
  PageActionButton,
  PageShell,
  StatusBadge,
} from '../components/ui'
import {
  downloadAdvancedReport,
  getAdvancedReport,
  type AdvancedReport,
  type ReportBreakdownItem,
  type ReportExportFormat,
} from '../services/reportService'
import { formatStatus, priorityVariant, statusVariant } from '../lib/formatters'
import { getLoadErrorMessage } from '../lib/errors'

type RangePreset = '30' | '90' | '365' | 'custom'

type ReportRange = {
  fromDate: string
  toDate: string
}

const DAY_IN_MS = 24 * 60 * 60 * 1000

const tooltipStyle: CSSProperties = {
  background: 'var(--crm-surface-elevated)',
  border: '1px solid var(--crm-border)',
  borderRadius: '12px',
  color: 'var(--crm-text)',
  boxShadow: 'var(--crm-shadow-soft)',
}

const taskStatusColors: Record<string, string> = {
  OPEN: '#2563eb',
  IN_PROGRESS: '#7c3aed',
  COMPLETED: '#10b981',
  CANCELLED: '#ef4444',
}

function toInputDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function rangeForDays(days: number): ReportRange {
  const to = new Date()
  const from = new Date()
  from.setDate(from.getDate() - (days - 1))

  return {
    fromDate: toInputDate(from),
    toDate: toInputDate(to),
  }
}

const initialRange = rangeForDays(30)

function parseInputDate(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function startOfDayIso(value: string) {
  return parseInputDate(value).toISOString()
}

function endExclusiveIso(value: string) {
  const date = parseInputDate(value)
  date.setDate(date.getDate() + 1)
  return date.toISOString()
}

function formatShortDate(value: string) {
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
  }).format(new Date(`${value}T00:00:00`))
}

function reportHasData(report: AdvancedReport) {
  return [
    report.customersCreated,
    report.leadsCreated,
    report.leadConversions,
    report.tasksCreated,
    report.taskCompletions,
    report.activitiesRecorded,
  ].some((value) => value > 0)
}

function escapeCsvCell(value: string | number) {
  return `"${String(value).replaceAll('"', '""')}"`
}

function downloadReportCsv(report: AdvancedReport) {
  const rows: Array<Array<string | number>> = [
    ['Tadamun CRM report', ''],
    ['From', report.from],
    ['To', report.to],
    [],
    ['Metric', 'Count'],
    ['Customers created', report.customersCreated],
    ['Leads created', report.leadsCreated],
    ['Lead conversions', report.leadConversions],
    ['Tasks created', report.tasksCreated],
    ['Task completions', report.taskCompletions],
    ['Activities recorded', report.activitiesRecorded],
    ['Customer activities', report.customerActivities],
    [],
    ['Lead status', 'Count'],
    ...report.leadStatusBreakdown.map((item) => [formatStatus(item.key), item.count]),
    [],
    ['Task status', 'Count'],
    ...report.taskStatusBreakdown.map((item) => [formatStatus(item.key), item.count]),
    [],
    ['Task priority', 'Count'],
    ...report.taskPriorityBreakdown.map((item) => [formatStatus(item.key), item.count]),
  ]

  const csv = rows
    .map((row) => row.map(escapeCsvCell).join(','))
    .join('\r\n')

  const blob = new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `tadamun-report-${toInputDate(new Date())}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

function breakdownHasData(items: ReportBreakdownItem[]) {
  return items.some((item) => item.count > 0)
}
function hasReportExportPermission() {
  const storedUser = localStorage.getItem('user')

  if (!storedUser) return false

  try {
    const user = JSON.parse(storedUser) as {
      permissions?: string[]
    }

    return user.permissions?.includes('REPORT_EXPORT') === true
  } catch {
    return false
  }
}

function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = url
  link.download = fileName

  document.body.appendChild(link)
  link.click()
  link.remove()

  URL.revokeObjectURL(url)
}

export function ReportsPage() {
  const [preset, setPreset] = useState<RangePreset>('30')
  const [fromDate, setFromDate] = useState(initialRange.fromDate)
  const [toDate, setToDate] = useState(initialRange.toDate)
  const [requestedRange, setRequestedRange] = useState<ReportRange>(initialRange)
  const [requestVersion, setRequestVersion] = useState(0)
  const [report, setReport] = useState<AdvancedReport | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [filterError, setFilterError] = useState('')
  const [exportingFormat, setExportingFormat] =
    useState<ReportExportFormat | null>(null)

  const canExportReports = hasReportExportPermission()

  useEffect(() => {
    let ignore = false

    getAdvancedReport(
      startOfDayIso(requestedRange.fromDate),
      endExclusiveIso(requestedRange.toDate),
    )
      .then((data) => {
        if (!ignore) {
          setReport(data)
        }
      })
      .catch(() => {
        if (!ignore) {
          setReport(null)
          setLoadError(getLoadErrorMessage('report'))
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
  }, [requestedRange, requestVersion])

  function handlePresetChange(value: RangePreset) {
    setPreset(value)

    if (value !== 'custom') {
      const range = rangeForDays(Number(value))
      setFromDate(range.fromDate)
      setToDate(range.toDate)
    }
  }

  function applyRange(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFilterError('')

    if (!fromDate || !toDate) {
      setFilterError('Choose both a start date and an end date.')
      return
    }

    const from = parseInputDate(fromDate)
    const to = parseInputDate(toDate)

    if (from > to) {
      setFilterError('Start date must be on or before the end date.')
      return
    }

    const days = Math.floor((to.getTime() - from.getTime()) / DAY_IN_MS) + 1

    if (days > 366) {
      setFilterError('Report range cannot exceed 366 days.')
      return
    }

    setLoadError('')
    setLoading(true)
    setReport(null)
    setRequestedRange({ fromDate, toDate })
  }

  function retryReport() {
    setLoadError('')
    setLoading(true)
    setReport(null)
    setRequestVersion((current) => current + 1)
  }
  async function exportServerReport(format: ReportExportFormat) {
    setExportingFormat(format)

    try {
      const blob = await downloadAdvancedReport(
        format,
        startOfDayIso(requestedRange.fromDate),
        endExclusiveIso(requestedRange.toDate),
      )

      const extension = format === 'excel' ? 'xlsx' : 'pdf'

      saveBlob(
        blob,
        `tadamun-report-${requestedRange.fromDate}-to-${requestedRange.toDate}.${extension}`,
      )

      toast.success(
        `${format === 'excel' ? 'Excel' : 'PDF'} report downloaded`,
      )
    } catch {
      toast.error('Could not download the report. Please try again.')
    } finally {
      setExportingFormat(null)
    }
  }

  const hasData = report ? reportHasData(report) : false
  const leadHasData = report ? breakdownHasData(report.leadStatusBreakdown) : false
  const taskHasData = report ? breakdownHasData(report.taskStatusBreakdown) : false
  const priorityHasData = report ? breakdownHasData(report.taskPriorityBreakdown) : false

  return (
    <AppLayout>
      <PageShell
        title="Reports"
        description="Compare customer, lead, task, and activity results for a selected period."
        action={
          report && canExportReports ? (
            <div className="flex flex-wrap gap-2">
              <PageActionButton
                icon={Download}
                onClick={() => downloadReportCsv(report)}
              >
                CSV
              </PageActionButton>

              <button
                type="button"
                disabled={exportingFormat !== null}
                onClick={() => exportServerReport('excel')}
                className="crm-focus inline-flex h-11 items-center justify-center gap-2 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-4 text-sm font-semibold text-[var(--crm-text)] transition hover:bg-[var(--crm-surface-soft)] disabled:cursor-not-allowed disabled:opacity-60"
              >
                <FileSpreadsheet size={17} />
                {exportingFormat === 'excel' ? 'Preparing...' : 'Excel'}
              </button>

              <button
                type="button"
                disabled={exportingFormat !== null}
                onClick={() => exportServerReport('pdf')}
                className="crm-focus inline-flex h-11 items-center justify-center gap-2 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-4 text-sm font-semibold text-[var(--crm-text)] transition hover:bg-[var(--crm-surface-soft)] disabled:cursor-not-allowed disabled:opacity-60"
              >
                <FileText size={17} />
                {exportingFormat === 'pdf' ? 'Preparing...' : 'PDF'}
              </button>
            </div>
          ) : undefined
        }
      >
        <GlassCard>
          <form
            onSubmit={applyRange}
            className="grid gap-4 lg:grid-cols-[minmax(10rem,0.8fr)_1fr_1fr_auto] lg:items-end"
          >
            <label className="block">
              <span className="text-xs font-semibold uppercase text-[var(--crm-text-muted)]">
                Period
              </span>
              <select
                value={preset}
                onChange={(event) => handlePresetChange(event.target.value as RangePreset)}
                className="crm-focus mt-2 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
              >
                <option value="30">Last 30 days</option>
                <option value="90">Last 90 days</option>
                <option value="365">Last 365 days</option>
                <option value="custom">Custom range</option>
              </select>
            </label>

            <label className="block">
              <span className="text-xs font-semibold uppercase text-[var(--crm-text-muted)]">
                Start date
              </span>
              <input
                type="date"
                value={fromDate}
                onChange={(event) => {
                  setPreset('custom')
                  setFromDate(event.target.value)
                }}
                max={toDate}
                className="crm-focus mt-2 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
              />
            </label>

            <label className="block">
              <span className="text-xs font-semibold uppercase text-[var(--crm-text-muted)]">
                End date
              </span>
              <input
                type="date"
                value={toDate}
                onChange={(event) => {
                  setPreset('custom')
                  setToDate(event.target.value)
                }}
                min={fromDate}
                className="crm-focus mt-2 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
              />
            </label>

            <button
              type="submit"
              disabled={loading}
              className="crm-primary-action inline-flex h-11 items-center justify-center rounded-xl px-5 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? 'Loading...' : 'Apply'}
            </button>
          </form>

          {filterError && (
            <p className="mt-3 text-sm font-medium text-[var(--crm-danger-text)]" role="alert">
              {filterError}
            </p>
          )}
        </GlassCard>

        {loadError && <ErrorState message={loadError} onRetry={retryReport} />}

        {loading ? (
          <GlassCard>
            <LoadingState message="Loading report data..." />
          </GlassCard>
        ) : (
          report && (
            <>
              {!hasData && (
                <GlassCard>
                  <EmptyState
                    icon={BarChart3}
                    title="No activity in this period"
                    message="Choose a wider date range or add CRM records before running this report again."
                  />
                </GlassCard>
              )}

              <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                <MetricCard
                  label="Customers created"
                  value={report.customersCreated}
                  icon={UsersRound}
                  tone="green"
                />
                <MetricCard
                  label="Leads created"
                  value={report.leadsCreated}
                  icon={Target}
                  tone="blue"
                />
                <MetricCard
                  label="Lead conversions"
                  value={report.leadConversions}
                  icon={RefreshCw}
                  tone="green"
                />
                <MetricCard
                  label="Tasks created"
                  value={report.tasksCreated}
                  icon={ClipboardList}
                  tone="amber"
                />
                <MetricCard
                  label="Task completions"
                  value={report.taskCompletions}
                  icon={CheckCircle2}
                  tone="green"
                />
                <MetricCard
                  label="Activities recorded"
                  value={report.activitiesRecorded}
                  icon={Activity}
                  tone="slate"
                />
              </section>

              <GlassCard>
                <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <h2 className="font-semibold text-[var(--crm-text)]">Activity by day</h2>
                    <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
                      Recorded CRM actions during the selected period.
                    </p>
                  </div>
                  <NavLink
                    to="/audit-logs"
                    className="inline-flex items-center gap-1 text-sm font-semibold text-[var(--crm-primary)] hover:underline"
                  >
                    Open audit history <ArrowUpRight size={15} />
                  </NavLink>
                </div>

                {report.activitiesRecorded === 0 ? (
                  <EmptyState
                    icon={Activity}
                    title="No recorded activity"
                    message="No audited CRM actions occurred in this period."
                  />
                ) : (
                  <div className="h-72" role="img" aria-label="CRM activity count by day">
                    <ResponsiveContainer
                      width="100%"
                      height="100%"
                      initialDimension={{ width: 320, height: 288 }}
                    >
                      <LineChart data={report.dailyActivity} margin={{ left: -18, right: 12 }}>
                        <CartesianGrid stroke="var(--crm-chart-grid)" vertical={false} />
                        <XAxis
                          dataKey="date"
                          tickFormatter={formatShortDate}
                          tick={{ fill: 'var(--crm-text-muted)', fontSize: 12 }}
                          axisLine={false}
                          tickLine={false}
                          minTickGap={24}
                        />
                        <YAxis
                          allowDecimals={false}
                          tick={{ fill: 'var(--crm-text-muted)', fontSize: 12 }}
                          axisLine={false}
                          tickLine={false}
                        />
                        <Tooltip contentStyle={tooltipStyle} />
                        <Line
                          type="monotone"
                          dataKey="count"
                          name="Activity"
                          stroke="var(--crm-primary)"
                          strokeWidth={3}
                          dot={false}
                          activeDot={{ r: 5 }}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                )}
              </GlassCard>

              <section className="grid gap-4 xl:grid-cols-2">
                <GlassCard>
                  <div className="mb-5 flex items-center justify-between gap-3">
                    <div>
                      <h2 className="font-semibold text-[var(--crm-text)]">Lead status</h2>
                      <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
                        Leads created in this period, grouped by current status.
                      </p>
                    </div>
                    <NavLink
                      to="/leads"
                      className="text-sm font-semibold text-[var(--crm-primary)] hover:underline"
                    >
                      Open leads
                    </NavLink>
                  </div>

                  {leadHasData ? (
                    <>
                      <div className="h-72" role="img" aria-label="Leads grouped by status">
                        <ResponsiveContainer
                          width="100%"
                          height="100%"
                          initialDimension={{ width: 320, height: 288 }}
                        >
                          <BarChart
                            data={report.leadStatusBreakdown}
                            layout="vertical"
                            margin={{ left: 8, right: 16 }}
                          >
                            <CartesianGrid stroke="var(--crm-chart-grid)" horizontal={false} />
                            <XAxis type="number" allowDecimals={false} hide />
                            <YAxis
                              type="category"
                              dataKey="key"
                              tickFormatter={formatStatus}
                              width={86}
                              tick={{ fill: 'var(--crm-text-muted)', fontSize: 12 }}
                              axisLine={false}
                              tickLine={false}
                            />
                            <Tooltip contentStyle={tooltipStyle} />
                            <Bar dataKey="count" name="Leads" fill="var(--crm-primary)" radius={[0, 8, 8, 0]} />
                          </BarChart>
                        </ResponsiveContainer>
                      </div>
                      <ul className="mt-3 grid gap-2 sm:grid-cols-2">
                        {report.leadStatusBreakdown.map((item) => (
                          <li key={item.key} className="flex items-center justify-between gap-3 text-sm">
                            <StatusBadge variant={statusVariant(item.key)}>
                              {formatStatus(item.key)}
                            </StatusBadge>
                            <span className="font-semibold tabular-nums text-[var(--crm-text)]">
                              {item.count}
                            </span>
                          </li>
                        ))}
                      </ul>
                    </>
                  ) : (
                    <EmptyState
                      icon={Target}
                      title="No leads created"
                      message="No leads were created in this period."
                    />
                  )}
                </GlassCard>

                <GlassCard>
                  <div className="mb-5 flex items-center justify-between gap-3">
                    <div>
                      <h2 className="font-semibold text-[var(--crm-text)]">Task status</h2>
                      <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
                        Tasks created in this period, grouped by current status.
                      </p>
                    </div>
                    <NavLink
                      to="/tasks"
                      className="text-sm font-semibold text-[var(--crm-primary)] hover:underline"
                    >
                      Open tasks
                    </NavLink>
                  </div>

                  {taskHasData ? (
                    <>
                      <div className="h-72" role="img" aria-label="Tasks grouped by status">
                        <ResponsiveContainer
                          width="100%"
                          height="100%"
                          initialDimension={{ width: 320, height: 288 }}
                        >
                          <PieChart>
                            <Pie
                              data={report.taskStatusBreakdown}
                              dataKey="count"
                              nameKey="key"
                              innerRadius={62}
                              outerRadius={96}
                              paddingAngle={3}
                            >
                              {report.taskStatusBreakdown.map((item) => (
                                <Cell
                                  key={item.key}
                                  fill={taskStatusColors[item.key] ?? '#64748b'}
                                />
                              ))}
                            </Pie>
                            <Tooltip contentStyle={tooltipStyle} />
                          </PieChart>
                        </ResponsiveContainer>
                      </div>
                      <ul className="mt-3 grid gap-2 sm:grid-cols-2">
                        {report.taskStatusBreakdown.map((item) => (
                          <li key={item.key} className="flex items-center justify-between gap-3 text-sm">
                            <StatusBadge variant={statusVariant(item.key)}>
                              {formatStatus(item.key)}
                            </StatusBadge>
                            <span className="font-semibold tabular-nums text-[var(--crm-text)]">
                              {item.count}
                            </span>
                          </li>
                        ))}
                      </ul>
                    </>
                  ) : (
                    <EmptyState
                      icon={ClipboardList}
                      title="No tasks created"
                      message="No tasks were created in this period."
                    />
                  )}
                </GlassCard>
              </section>

              <GlassCard>
                <div className="mb-5">
                  <h2 className="font-semibold text-[var(--crm-text)]">Task priority</h2>
                  <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
                    Priority mix for tasks created in this period.
                  </p>
                </div>

                {priorityHasData ? (
                  <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                    {report.taskPriorityBreakdown.map((item) => (
                      <li
                        key={item.key}
                        className="flex items-center justify-between gap-3 rounded-xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4"
                      >
                        <StatusBadge variant={priorityVariant(item.key)}>
                          {formatStatus(item.key)}
                        </StatusBadge>
                        <span className="text-xl font-semibold tabular-nums text-[var(--crm-text)]">
                          {item.count}
                        </span>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <EmptyState
                    icon={ClipboardList}
                    title="No task priorities"
                    message="No tasks were created in this period."
                  />
                )}
              </GlassCard>
            </>
          )
        )}
      </PageShell>
    </AppLayout>
  )
}
