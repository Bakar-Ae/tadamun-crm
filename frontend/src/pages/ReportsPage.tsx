import { useEffect, useState } from 'react'
import { motion, type Variants } from 'framer-motion'
import {
  Archive,
  BarChart3,
  CheckCircle2,
  ClipboardList,
  Download,
  Target,
  UsersRound,
  XCircle,
} from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { EmptyState, GlassCard, MetricCard, PageActionButton, PageShell, StatTile } from '../components/ui'
import { getReportSummary, type ReportSummary } from '../services/reportService'
import { getLoadErrorMessage } from '../lib/errors'

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

function reportHasData(report: ReportSummary) {
  return Object.values(report).some((value) => Number(value) > 0)
}

function downloadReportCsv(report: ReportSummary) {
  const rows = Object.entries(report).map(([key, value]) => [key, String(value)])
  const csv = ['metric,value', ...rows.map((row) => row.join(','))].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'tadamun-report-summary.csv'
  link.click()
  URL.revokeObjectURL(url)
}

export function ReportsPage() {
  const [report, setReport] = useState<ReportSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false

    getReportSummary()
      .then((data) => {
        if (!ignore) {
          setReport(data)
        }
      })
      .catch(() => {
        if (!ignore) {
          setError(getLoadErrorMessage('report summary'))
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

  return (
    <AppLayout>
      <PageShell
        title="Reports"
        description="Review the current customer, lead, and task totals your team is working from."
        action={
          report && (
            <PageActionButton icon={Download} onClick={() => downloadReportCsv(report)}>
              Export CSV
            </PageActionButton>
          )
        }
      >
        {error && (
          <div className="rounded-2xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
            {error}
          </div>
        )}

        {loading ? (
          <GlassCard className="py-10 text-center text-sm text-[var(--crm-text-muted)]">
            Loading report summary...
          </GlassCard>
        ) : (
          report && (
            <>
              {!reportHasData(report) && (
                <GlassCard>
                  <EmptyState
                    icon={BarChart3}
                    title="No report data yet"
                    message="Reports will become useful after customers, leads, and tasks are added."
                  />
                </GlassCard>
              )}

              <motion.section
                className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3"
                variants={containerAnimation}
                initial="hidden"
                animate="show"
              >
                <motion.div variants={cardAnimation}>
                  <MetricCard
                    label="Customer accounts"
                    value={report.totalCustomers}
                    icon={UsersRound}
                    tone="green"
                    trend={`${report.activeCustomers} active`}
                  />
                </motion.div>

                <motion.div variants={cardAnimation}>
                  <MetricCard
                    label="Lead pipeline"
                    value={report.totalLeads}
                    icon={Target}
                    tone="blue"
                    trend={`${report.qualifiedLeads} qualified`}
                  />
                </motion.div>

                <motion.div variants={cardAnimation}>
                  <MetricCard
                    label="Team tasks"
                    value={report.totalTasks}
                    icon={ClipboardList}
                    tone="amber"
                    trend={`${report.completedTasks} completed`}
                  />
                </motion.div>
              </motion.section>

              <GlassCard>
                <div className="mb-5 flex items-center gap-3">
                  <div className="rounded-xl bg-[var(--crm-soft-gradient)] p-3 text-[var(--crm-primary)] ring-1 ring-violet-300/25">
                    <BarChart3 size={22} />
                  </div>
                  <div>
                    <h3 className="font-semibold text-[var(--crm-text)]">Operational snapshot</h3>
                    <p className="text-sm text-[var(--crm-text-muted)]">
                      These totals come from the current CRM database.
                    </p>
                  </div>
                </div>

                <motion.div
                  className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4"
                  variants={containerAnimation}
                  initial="hidden"
                  animate="show"
                >
                  <motion.div variants={cardAnimation}>
                    <StatTile
                      label="Archived customers"
                      value={report.archivedCustomers}
                      icon={Archive}
                      tone="amber"
                    />
                  </motion.div>

                  <motion.div variants={cardAnimation}>
                    <StatTile label="New leads" value={report.newLeads} icon={Target} tone="blue" />
                  </motion.div>

                  <motion.div variants={cardAnimation}>
                    <StatTile label="Lost leads" value={report.lostLeads} icon={XCircle} tone="red" />
                  </motion.div>

                  <motion.div variants={cardAnimation}>
                    <StatTile
                      label="Cancelled tasks"
                      value={report.cancelledTasks}
                      icon={CheckCircle2}
                      tone="red"
                    />
                  </motion.div>
                </motion.div>
              </GlassCard>
            </>
          )
        )}
      </PageShell>
    </AppLayout>
  )
}
