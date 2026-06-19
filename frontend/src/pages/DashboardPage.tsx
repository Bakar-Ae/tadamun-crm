import { useCallback, useEffect, useMemo, useState } from "react";
import { motion, type Variants } from "framer-motion";
import { Link } from "react-router";
import {
  ArrowRight,
  BarChart3,
  ClipboardList,
  Download,
  RefreshCw,
  Search,
  Target,
  Users,
  Zap,
} from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
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
import { GlassCard, PageShell } from "../components/ui";

const containerAnimation: Variants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.07,
    },
  },
};

const cardAnimation: Variants = {
  hidden: { opacity: 0, y: 14, scale: 0.99 },
  show: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: {
      duration: 0.28,
      ease: "easeOut",
    },
  },
};

function formatTime(value: Date | null) {
  if (!value) {
    return "-";
  }

  return value.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function buildSparkline(value: number) {
  const safeValue = Math.max(0, value);
  const steps = [0, 0.2, 0.38, 0.55, 0.78, 1];

  return steps.map((step, index) => ({
    name: String(index + 1),
    value: Math.round(safeValue * step),
  }));
}

type EmptyPanelProps = {
  icon: typeof Target;
  title: string;
  message: string;
  actionLabel: string;
  to: string;
};

function EmptyPanel({ icon: Icon, title, message, actionLabel, to }: EmptyPanelProps) {
  return (
    <div className="rounded-[1.5rem] border border-dashed border-violet-200 bg-violet-500/5 p-5">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex gap-3">
          <div className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)]">
            <Icon size={20} />
          </div>
          <div>
            <p className="font-semibold text-[var(--crm-text)]">{title}</p>
            <p className="mt-1 text-sm leading-6 text-[var(--crm-text-muted)]">
              {message}
            </p>
          </div>
        </div>
        <Link
          to={to}
          className="inline-flex h-10 shrink-0 items-center justify-center gap-2 rounded-2xl bg-[var(--crm-brand-gradient)] px-4 text-sm font-semibold text-white shadow-[0_14px_34px_rgba(109,93,251,0.22)]"
        >
          {actionLabel}
          <ArrowRight size={15} />
        </Link>
      </div>
    </div>
  );
}

type KpiCardProps = {
  label: string;
  value: string | number;
  caption: string;
  icon: typeof Users;
  path: string;
  sparkline: Array<{ name: string; value: number }>;
};

function KpiCard({ label, value, caption, icon: Icon, path, sparkline }: KpiCardProps) {
  return (
    <motion.div variants={cardAnimation} whileHover={{ y: -4, scale: 1.01 }}>
      <Link to={path} className="block h-full">
        <GlassCard className="relative h-full overflow-hidden p-5 transition hover:border-violet-300/70">
          <div className="absolute -right-10 -top-10 h-28 w-28 rounded-full bg-violet-500/12 blur-2xl" />
          <div className="relative flex items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--crm-text-muted)]">
                {label}
              </p>
              <p className="mt-3 text-3xl font-semibold text-[var(--crm-text)]">
                {value}
              </p>
            </div>
            <div className="grid h-12 w-12 place-items-center rounded-2xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)] ring-1 ring-violet-300/25">
              <Icon size={21} />
            </div>
          </div>

          <div className="relative mt-4 flex items-center justify-between gap-3">
            <span className="text-xs font-semibold text-[var(--crm-text-muted)]">
              {caption}
            </span>
            <span className="inline-flex items-center gap-1 text-xs font-bold text-[var(--crm-accent-text)]">
              Open
              <ArrowRight size={13} />
            </span>
          </div>

          <div className="relative mt-4 h-14" aria-hidden="true">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={sparkline}>
                <defs>
                  <linearGradient id={`spark-${label}`} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#6D5DFB" stopOpacity={0.34} />
                    <stop offset="100%" stopColor="#38BDF8" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <Area
                  type="monotone"
                  dataKey="value"
                  stroke="#6D5DFB"
                  strokeWidth={3}
                  fill={`url(#spark-${label})`}
                  dot={false}
                  isAnimationActive
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </GlassCard>
      </Link>
    </motion.div>
  );
}

type PipelineCardProps = {
  summary: DashboardSummary | null;
};

function PipelineCard({ summary }: PipelineCardProps) {
  const rows = [
    {
      label: "Active customers",
      value: summary?.activeCustomers ?? 0,
      path: "/customers",
    },
    {
      label: "Active leads",
      value: summary?.activeLeads ?? 0,
      path: "/leads",
    },
    {
      label: "Archived customers",
      value: summary?.archivedCustomers ?? 0,
      path: "/customers",
    },
  ];
  const max = Math.max(...rows.map((row) => row.value), 1);
  const hasData = rows.some((row) => row.value > 0);

  return (
    <motion.div variants={cardAnimation}>
      <GlassCard className="relative min-h-[380px] overflow-hidden">
        <div className="absolute -right-24 top-12 h-56 w-56 rounded-full bg-violet-500/10 blur-3xl" />
        <div className="relative flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--crm-text-muted)]">
              Pipeline
            </p>
            <h2 className="mt-2 text-2xl font-semibold text-[var(--crm-text)]">
              Sales pipeline
            </h2>
            <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
              Real customer and lead counts from the backend.
            </p>
          </div>
          <Link
            to="/leads"
            className="inline-flex h-10 items-center gap-2 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm font-semibold text-[var(--crm-text)] shadow-sm transition hover:border-violet-300"
          >
            Manage
            <ArrowRight size={15} />
          </Link>
        </div>

        {hasData ? (
          <div className="relative mt-8 space-y-5">
            {rows.map((row, index) => {
              const width = Math.max(8, (row.value / max) * 100);

              return (
                <Link key={row.label} to={row.path} className="block">
                  <div className="mb-2 flex items-center justify-between text-sm">
                    <span className="font-semibold text-[var(--crm-text)]">
                      {row.label}
                    </span>
                    <span className="font-semibold text-[var(--crm-text-muted)]">
                      {row.value}
                    </span>
                  </div>
                  <div className="h-11 overflow-hidden rounded-2xl bg-violet-500/8">
                    <motion.div
                      className="flex h-full items-center justify-end rounded-2xl bg-gradient-to-r from-[#A78BFA] via-[#6D5DFB] to-[#38BDF8] pr-4 text-xs font-bold text-white shadow-[0_12px_30px_rgba(109,93,251,0.22)]"
                      initial={{ width: 0 }}
                      animate={{ width: `${width}%` }}
                      transition={{ duration: 0.55, delay: index * 0.07, ease: "easeOut" }}
                    >
                      {row.value}
                    </motion.div>
                  </div>
                </Link>
              );
            })}
          </div>
        ) : (
          <div className="relative mt-8">
            <EmptyPanel
              icon={Target}
              title="No pipeline data yet"
              message="Create your first lead or customer so the dashboard can start showing useful movement."
              actionLabel="Open leads"
              to="/leads"
            />
          </div>
        )}
      </GlassCard>
    </motion.div>
  );
}

type TaskFocusCardProps = {
  openTasks: number;
  completedTasks: number;
};

function TaskFocusCard({ openTasks, completedTasks }: TaskFocusCardProps) {
  const totalTasks = openTasks + completedTasks;
  const completionRate =
    totalTasks === 0 ? 0 : Math.round((completedTasks / totalTasks) * 100);

  return (
    <motion.div variants={cardAnimation}>
      <GlassCard className="min-h-[380px]">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--crm-text-muted)]">
              Follow-ups
            </p>
            <h2 className="mt-2 text-2xl font-semibold text-[var(--crm-text)]">
              Task focus
            </h2>
            <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
              Open and completed work your team needs to move.
            </p>
          </div>
          <div className="grid h-11 w-11 place-items-center rounded-2xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)]">
            <Zap size={20} />
          </div>
        </div>

        {totalTasks > 0 ? (
          <>
            <div className="mt-8 grid place-items-center">
              <div
                className="grid h-44 w-44 place-items-center rounded-full shadow-[0_18px_44px_rgba(109,93,251,0.18)]"
                style={{
                  background: `conic-gradient(#6D5DFB ${completionRate * 3.6}deg, rgba(109, 93, 251, 0.12) 0deg)`,
                }}
              >
                <div className="grid h-32 w-32 place-items-center rounded-full bg-[var(--crm-surface)] text-center shadow-inner">
                  <div>
                    <p className="text-4xl font-semibold text-[var(--crm-text)]">
                      {completionRate}%
                    </p>
                    <p className="mt-1 text-xs font-bold uppercase tracking-[0.12em] text-[var(--crm-text-muted)]">
                      Complete
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-7 grid gap-3 sm:grid-cols-2">
              <Link
                to="/tasks"
                className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4 transition hover:border-violet-300"
              >
                <p className="text-sm text-[var(--crm-text-muted)]">Open tasks</p>
                <p className="mt-1 text-2xl font-semibold text-[var(--crm-text)]">
                  {openTasks}
                </p>
              </Link>
              <Link
                to="/tasks"
                className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4 transition hover:border-violet-300"
              >
                <p className="text-sm text-[var(--crm-text-muted)]">Completed</p>
                <p className="mt-1 text-2xl font-semibold text-[var(--crm-text)]">
                  {completedTasks}
                </p>
              </Link>
            </div>
          </>
        ) : (
          <div className="mt-8">
            <EmptyPanel
              icon={ClipboardList}
              title="No tasks yet"
              message="Create tasks for calls, follow-ups, and internal work so the team always knows the next move."
              actionLabel="Open tasks"
              to="/tasks"
            />
          </div>
        )}
      </GlassCard>
    </motion.div>
  );
}

type SalesRow = {
  name: string;
  metric: string;
  value: string | number;
  signal: string;
  path: string;
};

function SalesTable({ rows }: { rows: SalesRow[] }) {
  const [query, setQuery] = useState("");
  const normalizedQuery = query.trim().toLowerCase();
  const visibleRows = normalizedQuery
    ? rows.filter((row) =>
        [row.name, row.metric, row.value, row.signal].some((field) =>
          String(field).toLowerCase().includes(normalizedQuery),
        ),
      )
    : rows;

  return (
    <motion.div variants={cardAnimation}>
      <GlassCard className="overflow-hidden p-0">
        <div className="flex flex-col justify-between gap-4 border-b border-[var(--crm-border)] p-5 sm:flex-row sm:items-center">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--crm-text-muted)]">
              Modules
            </p>
            <h2 className="mt-2 text-xl font-semibold text-[var(--crm-text)]">
              CRM operating table
            </h2>
          </div>
          <div className="relative">
            <Search
              size={16}
              className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--crm-text-muted)]"
            />
            <input
              className="crm-focus h-10 w-full rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] pl-9 pr-3 text-sm text-[var(--crm-text)] shadow-sm sm:w-56"
              placeholder="Search modules"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              aria-label="Search dashboard modules"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase tracking-[0.08em] text-[var(--crm-text-muted)]">
              <tr>
                <th className="px-5 py-3 font-semibold">Area</th>
                <th className="px-5 py-3 font-semibold">Metric</th>
                <th className="px-5 py-3 font-semibold">Value</th>
                <th className="px-5 py-3 font-semibold">Signal</th>
                <th className="px-5 py-3 font-semibold"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--crm-border)]">
              {visibleRows.map((row) => (
                <motion.tr
                  key={row.name}
                  className="transition hover:bg-violet-500/5"
                  whileHover={{ x: 2 }}
                >
                  <td className="px-5 py-4 font-semibold text-[var(--crm-text)]">
                    {row.name}
                  </td>
                  <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                    {row.metric}
                  </td>
                  <td className="px-5 py-4 text-[var(--crm-text)]">{row.value}</td>
                  <td className="px-5 py-4">
                    <span className="rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-bold text-[var(--crm-success-text)]">
                      {row.signal}
                    </span>
                  </td>
                  <td className="px-5 py-4 text-right">
                    <Link
                      to={row.path}
                      className="inline-flex items-center gap-1 text-xs font-bold text-[var(--crm-accent-text)]"
                    >
                      Open
                      <ArrowRight size={14} />
                    </Link>
                  </td>
                </motion.tr>
              ))}
              {visibleRows.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-5 py-8 text-center text-sm text-[var(--crm-text-muted)]"
                  >
                    No matching CRM area found.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </GlassCard>
    </motion.div>
  );
}

export function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [user, setUser] = useState<LoginResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(false);

  const loadDashboard = useCallback(() => {
    setLoading(true);

    Promise.all([getDashboardSummary(), getMe()])
      .then(([summaryData, userData]) => {
        setSummary(summaryData);
        setUser(userData);
        setUpdatedAt(new Date());
      })
      .catch(() => {
        localStorage.removeItem("token");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("user");
        window.location.href = "/";
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    let ignore = false;

    Promise.all([getDashboardSummary(), getMe()])
      .then(([summaryData, userData]) => {
        if (!ignore) {
          setSummary(summaryData);
          setUser(userData);
          setUpdatedAt(new Date());
        }
      })
      .catch(() => {
        if (!ignore) {
          localStorage.removeItem("token");
          localStorage.removeItem("refreshToken");
          localStorage.removeItem("user");
          window.location.href = "/";
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    if (!autoRefresh) {
      return undefined;
    }

    const interval = window.setInterval(loadDashboard, 30000);
    return () => window.clearInterval(interval);
  }, [autoRefresh, loadDashboard]);

  const activeCustomers = summary?.activeCustomers ?? 0;
  const archivedCustomers = summary?.archivedCustomers ?? 0;
  const activeLeads = summary?.activeLeads ?? 0;
  const openTasks = summary?.openTasks ?? 0;
  const completedTasks = summary?.completedTasks ?? 0;
  const totalUsers = summary?.totalUsers ?? 0;
  const taskTotal = openTasks + completedTasks;
  const completionRate =
    taskTotal === 0 ? 0 : Math.round((completedTasks / taskTotal) * 100);
  const hasRealActivity = Boolean(
    activeCustomers + archivedCustomers + activeLeads + openTasks + completedTasks,
  );

  const greeting = (() => {
    const hour = new Date().getHours();

    if (hour < 12) {
      return "Good morning";
    }

    if (hour < 18) {
      return "Good afternoon";
    }

    return "Good evening";
  })();

  const kpis = [
    {
      label: "Users",
      value: loading ? "-" : totalUsers,
      caption: "Team accounts",
      icon: Users,
      path: "/users",
      sparkline: buildSparkline(totalUsers),
    },
    {
      label: "Customers",
      value: loading ? "-" : activeCustomers,
      caption: "Active accounts",
      icon: BarChart3,
      path: "/customers",
      sparkline: buildSparkline(activeCustomers),
    },
    {
      label: "Leads",
      value: loading ? "-" : activeLeads,
      caption: "Open pipeline",
      icon: Target,
      path: "/leads",
      sparkline: buildSparkline(activeLeads),
    },
    {
      label: "Open Tasks",
      value: loading ? "-" : openTasks,
      caption: `${completedTasks} completed`,
      icon: ClipboardList,
      path: "/tasks",
      sparkline: buildSparkline(openTasks),
    },
  ];

  const workloadData = useMemo(
    () => [
      { name: "Customers", value: activeCustomers },
      { name: "Leads", value: activeLeads },
      { name: "Open", value: openTasks },
      { name: "Done", value: completedTasks },
    ],
    [activeCustomers, activeLeads, openTasks, completedTasks],
  );
  const hasWorkloadData = workloadData.some((item) => item.value > 0);

  const tableRows: SalesRow[] = [
    {
      name: "Customers",
      metric: "Active accounts",
      value: activeCustomers,
      signal: activeCustomers > 0 ? "Live" : "Empty",
      path: "/customers",
    },
    {
      name: "Leads",
      metric: "Open pipeline",
      value: activeLeads,
      signal: activeLeads > 0 ? "Live" : "Empty",
      path: "/leads",
    },
    {
      name: "Tasks",
      metric: "Open follow-ups",
      value: openTasks,
      signal: openTasks > 0 ? "Action" : "Clear",
      path: "/tasks",
    },
    {
      name: "Team",
      metric: "Users",
      value: totalUsers,
      signal: totalUsers > 0 ? "Ready" : "Empty",
      path: "/users",
    },
  ];

  function exportDashboardSummary() {
    if (!summary) {
      return;
    }

    const rows = [
      ["Metric", "Value"],
      ["Total Users", summary.totalUsers],
      ["Active Customers", summary.activeCustomers],
      ["Archived Customers", summary.archivedCustomers],
      ["Active Leads", summary.activeLeads],
      ["Open Tasks", summary.openTasks],
      ["Completed Tasks", summary.completedTasks],
      ["Task Completion Rate", `${completionRate}%`],
      ["Exported At", new Date().toISOString()],
    ];
    const csv = rows
      .map((row) =>
        row
          .map((value) => `"${String(value).replaceAll('"', '""')}"`)
          .join(","),
      )
      .join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = `tadamun-dashboard-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  return (
    <AppLayout>
      <PageShell
        title={`${greeting}, ${user?.fullName?.split(" ")[0] ?? "Admin"}`}
        description="Customers, leads, tasks, and team activity without noise."
        action={
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={loadDashboard}
              disabled={loading}
              className="inline-flex h-10 items-center gap-2 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm font-semibold text-[var(--crm-text)] shadow-sm transition hover:border-violet-300 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <RefreshCw size={16} className={loading ? "animate-spin" : ""} />
              Refresh
            </button>
            <button
              type="button"
              onClick={() => setAutoRefresh((value) => !value)}
              className={`inline-flex h-10 items-center gap-2 rounded-2xl border px-3 text-sm font-semibold shadow-sm transition ${
                autoRefresh
                  ? "border-violet-300 bg-violet-500/10 text-[var(--crm-primary)]"
                  : "border-[var(--crm-border)] bg-[var(--crm-surface)] text-[var(--crm-text)]"
              }`}
              aria-pressed={autoRefresh}
            >
              <RefreshCw size={16} className={autoRefresh ? "animate-spin" : ""} />
              Auto refresh
            </button>
            <button
              type="button"
              onClick={exportDashboardSummary}
              disabled={loading || !summary}
              className="inline-flex h-10 items-center gap-2 rounded-2xl bg-[var(--crm-brand-gradient)] px-3 text-sm font-semibold text-white shadow-[0_14px_34px_rgba(109,93,251,0.22)] transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
            >
              <Download size={16} />
              Export
            </button>
          </div>
        }
      >
        {!hasRealActivity && (
          <motion.div variants={cardAnimation} initial="hidden" animate="show">
            <div className="rounded-[2rem] border border-violet-200/70 bg-gradient-to-r from-violet-500/10 via-white to-sky-400/10 p-5 shadow-[var(--crm-shadow-card)]">
              <EmptyPanel
                icon={Target}
                title="Start with one real record"
                message="Add a lead, customer, or task. The dashboard will stay simple until there is real work to summarize."
                actionLabel="Open leads"
                to="/leads"
              />
            </div>
          </motion.div>
        )}

        <motion.section
          className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          {kpis.map((kpi) => (
            <KpiCard
              key={kpi.label}
              label={kpi.label}
              value={kpi.value}
              caption={kpi.caption}
              icon={kpi.icon}
              path={kpi.path}
              sparkline={kpi.sparkline}
            />
          ))}
        </motion.section>

        <motion.section
          className="grid gap-4 xl:grid-cols-[1.05fr_0.95fr]"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <PipelineCard summary={summary} />
          <TaskFocusCard openTasks={openTasks} completedTasks={completedTasks} />
        </motion.section>

        <motion.section
          className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <GlassCard className="min-h-[360px]">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--crm-text-muted)]">
                    Workload
                  </p>
                  <h2 className="mt-2 text-xl font-semibold text-[var(--crm-text)]">
                    Activity summary
                  </h2>
                </div>
                <ClipboardList className="text-[var(--crm-primary)]" size={24} />
              </div>

              {hasWorkloadData ? (
                <div className="mt-6 h-60">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={workloadData}>
                      <CartesianGrid stroke="var(--crm-chart-grid)" vertical={false} />
                      <XAxis
                        dataKey="name"
                        tickLine={false}
                        axisLine={false}
                        stroke="var(--crm-text-muted)"
                      />
                      <YAxis
                        allowDecimals={false}
                        tickLine={false}
                        axisLine={false}
                        stroke="var(--crm-text-muted)"
                      />
                      <Tooltip
                        contentStyle={{
                          background: "var(--crm-surface)",
                          border: "1px solid var(--crm-border)",
                          borderRadius: "18px",
                          boxShadow: "var(--crm-shadow-card)",
                          color: "var(--crm-text)",
                        }}
                      />
                      <Bar dataKey="value" radius={[14, 14, 6, 6]} fill="#6D5DFB" />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <div className="mt-6">
                  <EmptyPanel
                    icon={BarChart3}
                    title="No activity to chart"
                    message="Charts appear only when there is real customer, lead, or task data."
                    actionLabel="Open customers"
                    to="/customers"
                  />
                </div>
              )}
            </GlassCard>
          </motion.div>

          <SalesTable rows={tableRows} />
        </motion.section>

        <div className="text-xs font-medium text-[var(--crm-text-muted)]">
          Last updated {formatTime(updatedAt)}
        </div>
      </PageShell>
    </AppLayout>
  );
}
