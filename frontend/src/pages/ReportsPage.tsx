import { useEffect, useState } from 'react'
import { motion, type Variants } from 'framer-motion'
import { Archive, BarChart3, CheckCircle2, ClipboardList, Target, UsersRound, XCircle } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { GlassCard, MetricCard, PageShell, StatTile } from '../components/ui'
import { getReportSummary, type ReportSummary } from '../services/reportService'

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
          setError('Could not load reports. Please try again.')
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
        description="View high-level Tadamun totals for customers, leads, and tasks."
      >
        {error && (
          <div className="rounded-2xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
            {error}
          </div>
        )}

        {loading ? (
          <GlassCard className="py-10 text-center text-sm text-[var(--crm-text-muted)]">
            Loading reports...
          </GlassCard>
        ) : (
          report && (
            <>
              <motion.section
                className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3"
                variants={containerAnimation}
                initial="hidden"
                animate="show"
              >
                <motion.div variants={cardAnimation}>
                  <MetricCard
                    label="Total Customers"
                    value={report.totalCustomers}
                    icon={UsersRound}
                    tone="green"
                    trend="Customer base"
                  />
                </motion.div>

                <motion.div variants={cardAnimation}>
                  <MetricCard
                    label="Active Customers"
                    value={report.activeCustomers}
                    icon={UsersRound}
                    tone="blue"
                    trend="Current accounts"
                  />
                </motion.div>

                <motion.div variants={cardAnimation}>
                  <MetricCard
                    label="Total Leads"
                    value={report.totalLeads}
                    icon={Target}
                    tone="blue"
                    trend="Pipeline volume"
                  />
                </motion.div>

                <motion.div variants={cardAnimation}>
                  <MetricCard
                    label="Qualified Leads"
                    value={report.qualifiedLeads}
                    icon={Target}
                    tone="green"
                    trend="Sales ready"
                  />
                </motion.div>

                <motion.div variants={cardAnimation}>
                  <MetricCard
                    label="Total Tasks"
                    value={report.totalTasks}
                    icon={ClipboardList}
                    tone="amber"
                    trend="Execution load"
                  />
                </motion.div>

                <motion.div variants={cardAnimation}>
                  <MetricCard
                    label="Completed Tasks"
                    value={report.completedTasks}
                    icon={CheckCircle2}
                    tone="green"
                    trend="Work completed"
                  />
                </motion.div>
              </motion.section>

              <GlassCard>
                <div className="mb-5 flex items-center gap-3">
                  <div className="rounded-xl bg-cyan-400/10 p-3 text-[var(--crm-accent-text)] ring-1 ring-cyan-300/20">
                    <BarChart3 size={22} />
                  </div>
                  <div>
                    <h3 className="font-semibold text-[var(--crm-text)]">Detailed Breakdown</h3>
                    <p className="text-sm text-[var(--crm-text-muted)]">Additional report counters</p>
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
                      label="Archived Customers"
                      value={report.archivedCustomers}
                      icon={Archive}
                      tone="amber"
                    />
                  </motion.div>

                  <motion.div variants={cardAnimation}>
                    <StatTile label="New Leads" value={report.newLeads} icon={Target} tone="blue" />
                  </motion.div>

                  <motion.div variants={cardAnimation}>
                    <StatTile label="Lost Leads" value={report.lostLeads} icon={XCircle} tone="red" />
                  </motion.div>

                  <motion.div variants={cardAnimation}>
                    <StatTile
                      label="Cancelled Tasks"
                      value={report.cancelledTasks}
                      icon={XCircle}
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
