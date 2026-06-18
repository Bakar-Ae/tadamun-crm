import { useCallback, useEffect, useMemo, useState } from "react";
import { motion, type Variants } from "framer-motion";
import { Link } from "react-router";
import {
  ArrowRight,
  BarChart3,
  ChevronDown,
  ClipboardList,
  Download,
  Mail,
  RefreshCw,
  Search,
  Sparkles,
  Target,
  TrendingUp,
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

const dateRanges = [
  "Today",
  "Last 7 days",
  "Last 30 days",
  "Last 3 months",
  "Last 12 months",
  "Month to date",
  "Year to date",
  "All time",
] as const;

type DateRange = (typeof dateRanges)[number];

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
  hidden: { opacity: 0, y: 18, scale: 0.98 },
  show: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: {
      duration: 0.36,
      ease: "easeOut",
    },
  },
};

const previewSparkline = [
  { name: "1", value: 18 },
  { name: "2", value: 32 },
  { name: "3", value: 26 },
  { name: "4", value: 44 },
  { name: "5", value: 39 },
  { name: "6", value: 58 },
  { name: "7", value: 64 },
];

function formatCurrency(value: number) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0,
  }).format(value);
}

function formatTime(value: Date | null) {
  if (!value) {
    return "-";
  }

  return value.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function PreviewBadge() {
  return (
    <span className="inline-flex items-center rounded-full bg-violet-500/10 px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.12em] text-[var(--crm-accent-text)] ring-1 ring-violet-300/30">
      Preview
    </span>
  );
}

type KpiCardProps = {
  label: string;
  value: string;
  trend: string;
  icon: typeof Users;
  sparkline: Array<{ name: string; value: number }>;
  preview?: boolean;
};

function KpiCard({
  label,
  value,
  trend,
  icon: Icon,
  sparkline,
  preview = false,
}: KpiCardProps) {
  return (
    <motion.div variants={cardAnimation} whileHover={{ y: -5, scale: 1.01 }}>
      <GlassCard className="relative h-full overflow-hidden p-5">
        <div className="absolute -right-10 -top-10 h-28 w-28 rounded-full bg-violet-500/12 blur-2xl" />
        <div className="flex items-start justify-between gap-4">
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

        <div className="mt-4 flex items-center justify-between gap-3">
          <span className="rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-bold text-[var(--crm-success-text)]">
            {trend}
          </span>
          {preview && <PreviewBadge />}
        </div>

        <div className="mt-4 h-16">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={sparkline}>
              <defs>
                <linearGradient id={`spark-${label}`} x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#6D5DFB" stopOpacity={0.38} />
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
    </motion.div>
  );
}

type FunnelStage = {
  name: string;
  value: number;
};

function buildFunnelData(summary: DashboardSummary | null) {
  const leads = summary?.activeLeads ?? 0;

  if (leads === 0) {
    return {
      preview: true,
      data: [
        { name: "Qualified", value: 72 },
        { name: "Proposal", value: 54 },
        { name: "Negotiation", value: 36 },
        { name: "Contract", value: 22 },
        { name: "Won", value: 14 },
      ],
    };
  }

  return {
    preview: false,
    data: [
      { name: "Qualified", value: leads },
      { name: "Proposal", value: Math.max(1, Math.round(leads * 0.72)) },
      { name: "Negotiation", value: Math.max(1, Math.round(leads * 0.48)) },
      { name: "Contract", value: Math.max(1, Math.round(leads * 0.28)) },
      { name: "Won", value: Math.max(0, Math.round(leads * 0.16)) },
    ],
  };
}

function SalesFunnelCard({ stages, preview }: { stages: FunnelStage[]; preview: boolean }) {
  const max = Math.max(...stages.map((stage) => stage.value), 1);

  return (
    <motion.div variants={cardAnimation}>
      <GlassCard className="relative min-h-[440px] overflow-hidden">
        <div className="absolute -right-24 top-12 h-56 w-56 rounded-full bg-violet-500/10 blur-3xl" />
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
          <div>
            <div className="flex items-center gap-2">
              <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--crm-text-muted)]">
                Sales Funnel
              </p>
              {preview && <PreviewBadge />}
            </div>
            <h2 className="mt-2 text-2xl font-semibold text-[var(--crm-text)]">
              Pipeline stages
            </h2>
          </div>
          <button className="inline-flex h-10 items-center gap-2 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm font-semibold text-[var(--crm-text)] shadow-sm transition hover:border-violet-300">
            This month
            <ChevronDown size={16} />
          </button>
        </div>

        <div className="mt-8 space-y-4">
          {stages.map((stage, index) => {
            const width = Math.max(16, (stage.value / max) * 100);

            return (
              <div key={stage.name}>
                <div className="mb-2 flex items-center justify-between text-sm">
                  <span className="font-semibold text-[var(--crm-text)]">
                    {stage.name}
                  </span>
                  <span className="font-semibold text-[var(--crm-text-muted)]">
                    {stage.value}
                  </span>
                </div>
                <div className="h-11 overflow-hidden rounded-2xl bg-violet-500/8">
                  <motion.div
                    className="flex h-full items-center justify-end rounded-2xl bg-gradient-to-r from-[#A78BFA] via-[#6D5DFB] to-[#38BDF8] pr-4 text-xs font-bold text-white shadow-[0_12px_30px_rgba(109,93,251,0.22)]"
                    initial={{ width: 0 }}
                    animate={{ width: `${width}%` }}
                    transition={{ duration: 0.7, delay: index * 0.08, ease: "easeOut" }}
                  >
                    {Math.round(width)}%
                  </motion.div>
                </div>
              </div>
            );
          })}
        </div>
      </GlassCard>
    </motion.div>
  );
}

function EmailBubbleCard() {
  const bubbles = [
    { label: "Delivered", value: 84, size: "h-36 w-36", color: "bg-violet-500/18 text-[var(--crm-primary)]", x: 0 },
    { label: "Opened", value: 51, size: "h-28 w-28", color: "bg-cyan-400/20 text-sky-500", x: 22 },
    { label: "Undelivered", value: 6, size: "h-20 w-20", color: "bg-red-400/14 text-[var(--crm-danger-text)]", x: -18 },
  ];

  return (
    <motion.div variants={cardAnimation}>
      <GlassCard className="min-h-[440px]">
        <div className="flex items-start justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--crm-text-muted)]">
                Email Analytics
              </p>
              <PreviewBadge />
            </div>
            <h2 className="mt-2 text-2xl font-semibold text-[var(--crm-text)]">
              Engagement bubbles
            </h2>
          </div>
          <div className="grid h-11 w-11 place-items-center rounded-2xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)]">
            <Mail size={20} />
          </div>
        </div>

        <div className="mt-8 flex min-h-56 items-center justify-center gap-2">
          {bubbles.map((bubble, index) => (
            <motion.div
              key={bubble.label}
              className={`${bubble.size} ${bubble.color} grid place-items-center rounded-full text-center ring-1 ring-white/60`}
              initial={{ opacity: 0, scale: 0.6, y: 18 }}
              animate={{ opacity: 1, scale: 1, y: [0, -8, 0], x: bubble.x }}
              transition={{
                opacity: { duration: 0.35, delay: index * 0.1 },
                scale: { duration: 0.42, delay: index * 0.1 },
                y: { duration: 3, repeat: Infinity, ease: "easeInOut", delay: index * 0.25 },
                x: { duration: 0.42 },
              }}
            >
              <div>
                <p className="text-3xl font-semibold">{bubble.value}%</p>
                <p className="mt-1 text-xs font-bold uppercase tracking-[0.12em]">
                  {bubble.label}
                </p>
              </div>
            </motion.div>
          ))}
        </div>

        <div className="grid gap-3 sm:grid-cols-3">
          {bubbles.map((bubble) => (
            <div
              key={bubble.label}
              className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-3"
            >
              <p className="text-xs text-[var(--crm-text-muted)]">{bubble.label}</p>
              <p className="mt-1 text-lg font-semibold text-[var(--crm-text)]">
                {bubble.value}%
              </p>
            </div>
          ))}
        </div>
      </GlassCard>
    </motion.div>
  );
}

type SalesRow = {
  name: string;
  metric: string;
  value: string | number;
  status: string;
  path: string;
};

function SalesTable({ rows }: { rows: SalesRow[] }) {
  const [query, setQuery] = useState("");
  const normalizedQuery = query.trim().toLowerCase();
  const visibleRows = normalizedQuery
    ? rows.filter((row) =>
        [row.name, row.metric, row.value, row.status].some((field) =>
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
              Sales Data
            </p>
            <h2 className="mt-2 text-xl font-semibold text-[var(--crm-text)]">
              CRM operating table
            </h2>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search
                size={16}
                className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--crm-text-muted)]"
              />
              <input
                className="crm-focus h-10 w-56 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] pl-9 pr-3 text-sm text-[var(--crm-text)] shadow-sm"
                placeholder="Search table"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                aria-label="Search dashboard table"
              />
            </div>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-left text-sm">
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
                      {row.status}
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
  const [dateRange, setDateRange] = useState<DateRange>(() => {
    const savedRange = localStorage.getItem("crm-dashboard-date-range");

    return dateRanges.includes(savedRange as DateRange)
      ? (savedRange as DateRange)
      : "Last 30 days";
  });
  const [rangeOpen, setRangeOpen] = useState(false);
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

  useEffect(() => {
    localStorage.setItem("crm-dashboard-date-range", dateRange);
  }, [dateRange]);

  const hasRealActivity = Boolean(
    (summary?.activeCustomers ?? 0) +
      (summary?.activeLeads ?? 0) +
      (summary?.openTasks ?? 0) +
      (summary?.completedTasks ?? 0),
  );
  const taskTotal = (summary?.openTasks ?? 0) + (summary?.completedTasks ?? 0);
  const conversionRate =
    taskTotal === 0
      ? 0
      : Math.round(((summary?.completedTasks ?? 0) / taskTotal) * 100);
  const pipelineValue = (summary?.activeLeads ?? 0) * 2500;
  const funnel = useMemo(() => buildFunnelData(summary), [summary]);
  const tableRows: SalesRow[] = [
    {
      name: "Pipeline",
      metric: "Active leads",
      value: summary?.activeLeads ?? 0,
      status: "Tracked",
      path: "/leads",
    },
    {
      name: "Customers",
      metric: "Active accounts",
      value: summary?.activeCustomers ?? 0,
      status: "Tracked",
      path: "/customers",
    },
    {
      name: "Tasks",
      metric: "Open follow-ups",
      value: summary?.openTasks ?? 0,
      status: "Tracked",
      path: "/tasks",
    },
    {
      name: "Team",
      metric: "Users",
      value: summary?.totalUsers ?? 0,
      status: "Tracked",
      path: "/users",
    },
  ];
  const kpis = [
    {
      label: "Pipeline Value",
      value: loading ? "-" : formatCurrency(pipelineValue),
      trend: pipelineValue > 0 ? "+12.4%" : "Ready",
      icon: TrendingUp,
      preview: pipelineValue === 0,
    },
    {
      label: "CRM Revenue",
      value: loading ? "-" : formatCurrency(0),
      trend: "API pending",
      icon: BarChart3,
      preview: true,
    },
    {
      label: "Leads",
      value: loading ? "-" : String(summary?.activeLeads ?? 0),
      trend: (summary?.activeLeads ?? 0) > 0 ? "+8.2%" : "Ready",
      icon: Target,
      preview: (summary?.activeLeads ?? 0) === 0,
    },
    {
      label: "Conversion",
      value: loading ? "-" : `${conversionRate}%`,
      trend: taskTotal > 0 ? "+4.8%" : "Ready",
      icon: Zap,
      preview: taskTotal === 0,
    },
  ];

  function exportDashboardSummary() {
    if (!summary) {
      return;
    }

    const rows = [
      ["Metric", "Value"],
      ["Selected Range", dateRange],
      ["Total Users", summary.totalUsers],
      ["Active Customers", summary.activeCustomers],
      ["Archived Customers", summary.archivedCustomers],
      ["Active Leads", summary.activeLeads],
      ["Open Tasks", summary.openTasks],
      ["Completed Tasks", summary.completedTasks],
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
        title={`Good ${new Date().getHours() < 12 ? "morning" : new Date().getHours() < 18 ? "afternoon" : "evening"}, ${user?.fullName?.split(" ")[0] ?? "Admin"}`}
        description="Pipeline, customers, follow-ups, and team activity in one view."
        action={
          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <button
                type="button"
                onClick={() => setRangeOpen((value) => !value)}
                className="inline-flex h-10 items-center gap-2 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm font-semibold text-[var(--crm-text)] shadow-sm transition hover:border-violet-300"
              >
                {dateRange}
                <ChevronDown size={16} />
              </button>
              {rangeOpen && (
                <div className="absolute right-0 top-12 z-40 w-48 rounded-3xl border border-[var(--crm-border)] bg-[var(--crm-surface)] p-2 shadow-[var(--crm-shadow-soft)]">
                  {dateRanges.map((range) => (
                    <button
                      key={range}
                      type="button"
                      onClick={() => {
                        setDateRange(range);
                        setRangeOpen(false);
                      }}
                      className="w-full rounded-2xl px-3 py-2 text-left text-sm font-semibold text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-text)]"
                    >
                      {range}
                    </button>
                  ))}
                </div>
              )}
            </div>
            <button
              type="button"
              onClick={() => setAutoRefresh((value) => !value)}
              className={`inline-flex h-10 items-center gap-2 rounded-2xl border px-3 text-sm font-semibold shadow-sm transition ${
                autoRefresh
                  ? "border-violet-300 bg-violet-500/10 text-[var(--crm-primary)]"
                  : "border-[var(--crm-border)] bg-[var(--crm-surface)] text-[var(--crm-text)]"
              }`}
            >
              <RefreshCw size={16} className={autoRefresh ? "animate-spin" : ""} />
              Auto
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
            <div className="relative overflow-hidden rounded-[2rem] border border-violet-200/70 bg-gradient-to-r from-violet-500/12 via-white to-sky-400/12 p-5 shadow-[var(--crm-shadow-card)]">
              <div className="absolute right-8 top-4 h-24 w-24 rounded-full bg-violet-500/20 blur-3xl" />
              <div className="relative flex flex-col justify-between gap-4 md:flex-row md:items-center">
                <div>
                  <div className="inline-flex items-center gap-2 rounded-full bg-white/70 px-3 py-1 text-xs font-bold text-[var(--crm-primary)] ring-1 ring-violet-200">
                    <Sparkles size={14} />
                    Starter view
                  </div>
                  <h2 className="mt-3 text-xl font-semibold text-[var(--crm-text)]">
                    Add leads, customers, and tasks to activate live analytics.
                  </h2>
                  <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
                    Live counts are shown now. Chart previews fade out as CRM activity grows.
                  </p>
                </div>
                <Link
                  to="/leads"
                  className="inline-flex h-11 items-center justify-center gap-2 rounded-2xl bg-[var(--crm-brand-gradient)] px-4 text-sm font-semibold text-white shadow-[0_14px_34px_rgba(109,93,251,0.22)]"
                >
                  Open leads
                  <ArrowRight size={16} />
                </Link>
              </div>
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
              trend={kpi.trend}
              icon={kpi.icon}
              sparkline={previewSparkline}
              preview={kpi.preview}
            />
          ))}
        </motion.section>

        <motion.section
          className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <SalesFunnelCard stages={funnel.data} preview={funnel.preview} />
          <EmailBubbleCard />
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
                    Activity
                  </p>
                  <h2 className="mt-2 text-xl font-semibold text-[var(--crm-text)]">
                    Workload summary
                  </h2>
                </div>
                <ClipboardList className="text-[var(--crm-primary)]" size={24} />
              </div>

              <div className="mt-6 h-60">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={[
                      { name: "Customers", value: summary?.activeCustomers ?? 0 },
                      { name: "Leads", value: summary?.activeLeads ?? 0 },
                      { name: "Open", value: summary?.openTasks ?? 0 },
                      { name: "Done", value: summary?.completedTasks ?? 0 },
                    ]}
                  >
                    <CartesianGrid stroke="var(--crm-chart-grid)" vertical={false} />
                    <XAxis dataKey="name" tickLine={false} axisLine={false} stroke="var(--crm-text-muted)" />
                    <YAxis tickLine={false} axisLine={false} stroke="var(--crm-text-muted)" />
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
