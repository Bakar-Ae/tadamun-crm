import { useEffect, useMemo, useState } from "react";
import {
  Archive,
  CheckCircle2,
  ClipboardList,
  Target,
  TrendingUp,
  Users,
  UserRound,
} from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { AppLayout } from "../layouts/AppLayout";
import { getMe, type LoginResponse } from "../services/authService";
import {
  getDashboardSummary,
  type DashboardSummary,
} from "../services/dashboardService";
import {
  GlassCard,
  MetricCard,
  PageShell,
  StatusBadge,
} from "../components/ui";
import { motion, type Variants } from "framer-motion";

const containerAnimation: Variants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.08,
    },
  },
};

const cardAnimation: Variants = {
  hidden: { opacity: 0, y: 18 },
  show: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.35,
      ease: "easeOut",
    },
  },
};

export function DashboardPage() {
  const [user, setUser] = useState<LoginResponse | null>(null);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getMe(), getDashboardSummary()])
      .then(([userData, summaryData]) => {
        setUser(userData);
        setSummary(summaryData);
      })
      .catch(() => {
        localStorage.removeItem("token");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("user");
        window.location.href = "/";
      })
      .finally(() => setLoading(false));
  }, []);

  const chartData = useMemo(
    () => [
      { name: "Customers", value: summary?.activeCustomers ?? 0 },
      { name: "Leads", value: summary?.activeLeads ?? 0 },
      { name: "Open Tasks", value: summary?.openTasks ?? 0 },
      { name: "Done Tasks", value: summary?.completedTasks ?? 0 },
    ],
    [summary],
  );

  const pipelineData = useMemo(
    () => [
      { name: "Mon", leads: 4, customers: 2 },
      { name: "Tue", leads: 6, customers: 3 },
      { name: "Wed", leads: 5, customers: 4 },
      { name: "Thu", leads: 8, customers: 4 },
      { name: "Fri", leads: 7, customers: 5 },
      { name: "Sat", leads: 9, customers: 6 },
    ],
    [],
  );

  const taskPieData = useMemo(
    () => [
      { name: "Open", value: summary?.openTasks ?? 0, color: "#fdbf2d" },
      {
        name: "Completed",
        value: summary?.completedTasks ?? 0,
        color: "#02f5a1",
      },
    ],
    [summary],
  );

  return (
    <AppLayout>
      <PageShell
        title={`Welcome back${user?.fullName ? `, ${user.fullName}` : ""}`}
        description="Monitor customer growth, pipeline movement, task execution, and CRM health from one premium command center."
        action={
          <StatusBadge variant="info">{user?.role ?? "Loading"}</StatusBadge>
        }
      >
        <motion.section
          className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <MetricCard
              label="Active Customers"
              value={loading ? "-" : (summary?.activeCustomers ?? 0)}
              icon={UserRound}
              tone="green"
              trend="Live customer base"
            />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <MetricCard
              label="Active Leads"
              value={loading ? "-" : (summary?.activeLeads ?? 0)}
              icon={Target}
              tone="blue"
              trend="Pipeline in motion"
            />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <MetricCard
              label="Open Tasks"
              value={loading ? "-" : (summary?.openTasks ?? 0)}
              icon={ClipboardList}
              tone="amber"
              trend="Execution queue"
            />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <MetricCard
              label="Team Users"
              value={loading ? "-" : (summary?.totalUsers ?? 0)}
              icon={Users}
              tone="slate"
              trend="Workspace access"
            />
          </motion.div>
        </motion.section>

        <motion.section
          className="grid gap-4 xl:grid-cols-[1.35fr_0.65fr]"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <GlassCard className="min-h-[360px]">
              <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                <div>
                  <p className="text-sm font-semibold text-[var(--crm-accent-text)]">
                    Pipeline Momentum
                  </p>
                  <h2 className="mt-2 text-2xl font-semibold text-[var(--crm-text)]">
                    Sales activity trend
                  </h2>
                  <p className="mt-2 text-sm text-[var(--crm-text-muted)]">
                    Weekly lead and customer movement overview.
                  </p>
                </div>
                <StatusBadge variant="success">Healthy</StatusBadge>
              </div>

              <div className="mt-8 h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={pipelineData}>
                    <defs>
                      <linearGradient id="leads" x1="0" y1="0" x2="0" y2="1">
                        <stop
                          offset="5%"
                          stopColor="#41c0f2"
                          stopOpacity={0.45}
                        />
                        <stop
                          offset="95%"
                          stopColor="#41c0f2"
                          stopOpacity={0}
                        />
                      </linearGradient>
                      <linearGradient
                        id="customers"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="5%"
                          stopColor="#02f5a1"
                          stopOpacity={0.4}
                        />
                        <stop
                          offset="95%"
                          stopColor="#02f5a1"
                          stopOpacity={0}
                        />
                      </linearGradient>
                    </defs>
                    <CartesianGrid
                      stroke="var(--crm-chart-grid)"
                      vertical={false}
                    />
                    <XAxis
                      dataKey="name"
                      stroke="var(--crm-text-muted)"
                      tickLine={false}
                      axisLine={false}
                    />
                    <YAxis
                      stroke="var(--crm-text-muted)"
                      tickLine={false}
                      axisLine={false}
                    />
                    <Tooltip
                      contentStyle={{
                        background: "var(--crm-surface)",
                        border: "1px solid var(--crm-border)",
                        borderRadius: "12px",
                        color: "var(--crm-text)",
                      }}
                    />
                    <Area
                      type="monotone"
                      dataKey="leads"
                      stroke="#41c0f2"
                      fill="url(#leads)"
                      strokeWidth={3}
                    />
                    <Area
                      type="monotone"
                      dataKey="customers"
                      stroke="#02f5a1"
                      fill="url(#customers)"
                      strokeWidth={3}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </GlassCard>
          </motion.div>

          <motion.div variants={cardAnimation}>
            <GlassCard className="min-h-[360px]">
              <p className="text-sm font-semibold text-[var(--crm-accent-text)]">
                Task Status
              </p>
              <h2 className="mt-2 text-2xl font-semibold text-[var(--crm-text)]">
                Work completion
              </h2>
              <p className="mt-2 text-sm text-[var(--crm-text-muted)]">
                Open vs completed task balance.
              </p>

              <div className="mt-8 h-56">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={taskPieData}
                      dataKey="value"
                      nameKey="name"
                      innerRadius={58}
                      outerRadius={88}
                      paddingAngle={4}
                    >
                      {taskPieData.map((item) => (
                        <Cell key={item.name} fill={item.color} />
                      ))}
                    </Pie>
                    <Tooltip
                      contentStyle={{
                        background: "var(--crm-surface)",
                        border: "1px solid var(--crm-border)",
                        borderRadius: "12px",
                        color: "var(--crm-text)",
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              </div>

              <div className="mt-4 grid grid-cols-2 gap-3">
                <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
                  <p className="text-xs text-[var(--crm-text-muted)]">Open</p>
                  <p className="mt-1 text-2xl font-semibold text-[var(--crm-warning-text)]">
                    {summary?.openTasks ?? 0}
                  </p>
                </div>
                <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
                  <p className="text-xs text-[var(--crm-text-muted)]">
                    Completed
                  </p>
                  <p className="mt-1 text-2xl font-semibold text-[var(--crm-success-text)]">
                    {summary?.completedTasks ?? 0}
                  </p>
                </div>
              </div>
            </GlassCard>
          </motion.div>
        </motion.section>

        <motion.section
          className="grid gap-4 xl:grid-cols-[0.85fr_1.15fr]"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <GlassCard>
              <div className="flex items-center gap-3">
                <div className="rounded-2xl bg-cyan-400/10 p-3 text-[var(--crm-accent-text)] ring-1 ring-cyan-300/20">
                  <TrendingUp size={22} />
                </div>
                <div>
                  <p className="text-sm font-semibold text-[var(--crm-accent-text)]">
                    Tadamun Composition
                  </p>
                  <h2 className="text-xl font-semibold text-[var(--crm-text)]">
                    Workspace distribution
                  </h2>
                </div>
              </div>

              <div className="mt-6 h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData}>
                    <CartesianGrid
                      stroke="var(--crm-chart-grid)"
                      vertical={false}
                    />
                    <XAxis
                      dataKey="name"
                      stroke="var(--crm-text-muted)"
                      tickLine={false}
                      axisLine={false}
                    />
                    <YAxis
                      stroke="var(--crm-text-muted)"
                      tickLine={false}
                      axisLine={false}
                    />
                    <Tooltip
                      contentStyle={{
                        background: "var(--crm-surface)",
                        border: "1px solid var(--crm-border)",
                        borderRadius: "12px",
                        color: "var(--crm-text)",
                      }}
                    />
                    <Bar
                      dataKey="value"
                      radius={[10, 10, 0, 0]}
                      fill="#41c0f2"
                    />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </GlassCard>
          </motion.div>

          <motion.div variants={cardAnimation}>
            <GlassCard>
              <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                <div>
                  <p className="text-sm font-semibold text-[var(--crm-accent-text)]">
                    Operational Focus
                  </p>
                  <h2 className="mt-2 text-2xl font-semibold text-[var(--crm-text)]">
                    What needs attention
                  </h2>
                </div>
                <StatusBadge variant="neutral">
                  {user?.email ?? "Loading"}
                </StatusBadge>
              </div>

              <div className="mt-6 space-y-3">
                <div className="flex items-center justify-between rounded-2xl border border-amber-300/20 bg-amber-300/10 px-4 py-4">
                  <div className="flex items-center gap-3">
                    <ClipboardList
                      className="text-[var(--crm-warning-text)]"
                      size={20}
                    />
                    <div>
                      <p className="font-semibold text-[var(--crm-text)]">
                        Open tasks
                      </p>
                      <p className="text-sm text-[var(--crm-text-muted)]">
                        Work still waiting for execution
                      </p>
                    </div>
                  </div>
                  <p className="text-2xl font-semibold text-[var(--crm-warning-text)]">
                    {summary?.openTasks ?? 0}
                  </p>
                </div>

                <div className="flex items-center justify-between rounded-2xl border border-cyan-300/20 bg-cyan-300/10 px-4 py-4">
                  <div className="flex items-center gap-3">
                    <Target
                      className="text-[var(--crm-accent-text)]"
                      size={20}
                    />
                    <div>
                      <p className="font-semibold text-[var(--crm-text)]">
                        Active leads
                      </p>
                      <p className="text-sm text-[var(--crm-text-muted)]">
                        Pipeline opportunities to follow up
                      </p>
                    </div>
                  </div>
                  <p className="text-2xl font-semibold text-[var(--crm-accent-text)]">
                    {summary?.activeLeads ?? 0}
                  </p>
                </div>

                <div className="flex items-center justify-between rounded-2xl border border-red-300/20 bg-red-300/10 px-4 py-4">
                  <div className="flex items-center gap-3">
                    <Archive
                      className="text-[var(--crm-danger-text)]"
                      size={20}
                    />
                    <div>
                      <p className="font-semibold text-[var(--crm-text)]">
                        Archived customers
                      </p>
                      <p className="text-sm text-[var(--crm-text-muted)]">
                        Records removed from active workspace
                      </p>
                    </div>
                  </div>
                  <p className="text-2xl font-semibold text-[var(--crm-danger-text)]">
                    {summary?.archivedCustomers ?? 0}
                  </p>
                </div>

                <div className="flex items-center justify-between rounded-2xl border border-emerald-300/20 bg-emerald-300/10 px-4 py-4">
                  <div className="flex items-center gap-3">
                    <CheckCircle2
                      className="text-[var(--crm-success-text)]"
                      size={20}
                    />
                    <div>
                      <p className="font-semibold text-[var(--crm-text)]">
                        Completed tasks
                      </p>
                      <p className="text-sm text-[var(--crm-text-muted)]">
                        Execution progress this workspace can show
                      </p>
                    </div>
                  </div>
                  <p className="text-2xl font-semibold text-[var(--crm-success-text)]">
                    {summary?.completedTasks ?? 0}
                  </p>
                </div>
              </div>
            </GlassCard>
          </motion.div>
        </motion.section>
      </PageShell>
    </AppLayout>
  );
}
