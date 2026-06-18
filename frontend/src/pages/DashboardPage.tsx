import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { motion, type Variants } from "framer-motion";
import { Link } from "react-router";
import {
  Archive,
  ArrowRight,
  BarChart3,
  BriefcaseBusiness,
  CheckCircle2,
  ClipboardList,
  Download,
  RefreshCw,
  ShieldCheck,
  Target,
  Users,
} from "lucide-react";
import { AppLayout } from "../layouts/AppLayout";
import {
  getDashboardSummary,
  type DashboardSummary,
} from "../services/dashboardService";
import { GlassCard, PageShell } from "../components/ui";
import { getDashboardPreferences } from "../lib/dashboardPreferences";

const containerAnimation: Variants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.06,
    },
  },
};

const cardAnimation: Variants = {
  hidden: { opacity: 0, y: 12 },
  show: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.28,
      ease: "easeOut",
    },
  },
};

type KpiCardProps = {
  label: string;
  value: string | number;
  icon: typeof Users;
  path: string;
  tone: "cyan" | "green" | "amber" | "red" | "slate";
};

const tones = {
  cyan: "bg-cyan-400/10 text-cyan-200 ring-cyan-300/20",
  green: "bg-emerald-400/10 text-[var(--crm-success-text)] ring-emerald-300/20",
  amber: "bg-amber-400/10 text-[var(--crm-warning-text)] ring-amber-300/20",
  red: "bg-red-400/10 text-[var(--crm-danger-text)] ring-red-300/20",
  slate: "bg-slate-400/10 text-[var(--crm-text-muted)] ring-slate-300/20",
};

function KpiCard({ label, value, icon: Icon, path, tone }: KpiCardProps) {
  return (
    <Link to={path} className="group block">
      <GlassCard className="h-full p-4 transition group-hover:border-cyan-300/35">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--crm-text-muted)]">
              {label}
            </p>
            <p className="mt-3 text-3xl font-semibold text-[var(--crm-text)]">
              {value}
            </p>
          </div>
          <div className={`rounded-2xl p-3 ring-1 ${tones[tone]}`}>
            <Icon size={21} />
          </div>
        </div>
        <div className="mt-4 flex items-center gap-2 text-xs font-semibold text-[var(--crm-accent-text)] opacity-0 transition group-hover:opacity-100">
          Open module
          <ArrowRight size={14} />
        </div>
      </GlassCard>
    </Link>
  );
}

type ProgressRowProps = {
  label: string;
  value: number;
  max: number;
  tone: "cyan" | "green" | "amber" | "red";
  path: string;
};

const progressTones = {
  cyan: "from-cyan-300 to-cyan-500",
  green: "from-emerald-300 to-emerald-500",
  amber: "from-amber-300 to-amber-500",
  red: "from-red-300 to-red-500",
};

function ProgressRow({ label, value, max, tone, path }: ProgressRowProps) {
  const percent = max === 0 ? 0 : Math.round((value / max) * 100);

  return (
    <Link
      to={path}
      className="block rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4 transition hover:border-cyan-300/35 hover:bg-cyan-400/5"
    >
      <div className="flex items-center justify-between gap-3">
        <span className="text-sm font-semibold text-[var(--crm-text)]">
          {label}
        </span>
        <span className="text-sm font-semibold text-[var(--crm-text-muted)]">
          {value}
        </span>
      </div>
      <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/10">
        <div
          className={`h-full rounded-full bg-gradient-to-r ${progressTones[tone]}`}
          style={{ width: `${percent}%` }}
        />
      </div>
    </Link>
  );
}

type ActionRowProps = {
  icon: typeof Users;
  label: string;
  value: string | number;
  path: string;
  children?: ReactNode;
};

function ActionRow({ icon: Icon, label, value, path, children }: ActionRowProps) {
  return (
    <Link
      to={path}
      className="flex items-center justify-between gap-4 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] px-4 py-3 transition hover:border-cyan-300/35 hover:bg-cyan-400/5"
    >
      <div className="flex min-w-0 items-center gap-3">
        <div className="grid h-10 w-10 place-items-center rounded-xl bg-cyan-400/10 text-cyan-200 ring-1 ring-cyan-300/15">
          <Icon size={18} />
        </div>
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-[var(--crm-text)]">
            {label}
          </p>
          {children && (
            <p className="mt-1 truncate text-xs text-[var(--crm-text-muted)]">
              {children}
            </p>
          )}
        </div>
      </div>
      <div className="flex items-center gap-2">
        <span className="text-lg font-semibold text-[var(--crm-text)]">
          {value}
        </span>
        <ArrowRight size={16} className="text-[var(--crm-text-muted)]" />
      </div>
    </Link>
  );
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

export function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);
  const [preferences, setPreferences] = useState(getDashboardPreferences);

  const loadDashboard = useCallback(() => {
    setLoading(true);

    getDashboardSummary()
      .then((summaryData) => {
        setSummary(summaryData);
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

    getDashboardSummary()
      .then((summaryData) => {
        if (!ignore) {
          setSummary(summaryData);
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
    function syncPreferences() {
      setPreferences(getDashboardPreferences());
    }

    window.addEventListener("crm-dashboard-settings-changed", syncPreferences);
    return () =>
      window.removeEventListener(
        "crm-dashboard-settings-changed",
        syncPreferences,
      );
  }, []);

  const compact = preferences.density === "compact";
  const sectionGap = compact ? "gap-3" : "gap-4";
  const visibleWidgets = Object.values(preferences.widgets).some(Boolean);
  const totalTasks = (summary?.openTasks ?? 0) + (summary?.completedTasks ?? 0);
  const totalCustomers =
    (summary?.activeCustomers ?? 0) + (summary?.archivedCustomers ?? 0);

  const exportRows = useMemo(
    () => [
      ["Metric", "Value"],
      ["Total Users", summary?.totalUsers ?? 0],
      ["Active Customers", summary?.activeCustomers ?? 0],
      ["Archived Customers", summary?.archivedCustomers ?? 0],
      ["Active Leads", summary?.activeLeads ?? 0],
      ["Open Tasks", summary?.openTasks ?? 0],
      ["Completed Tasks", summary?.completedTasks ?? 0],
    ],
    [summary],
  );

  function exportDashboardSummary() {
    if (!summary) {
      return;
    }

    const csv = exportRows
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
        title="Dashboard"
        action={
          <div className="flex flex-wrap items-center gap-2">
            <div className="inline-flex h-10 items-center rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm font-semibold text-[var(--crm-text-muted)] shadow-sm">
              Updated {formatTime(updatedAt)}
            </div>
            <button
              type="button"
              onClick={loadDashboard}
              disabled={loading}
              className="inline-flex h-10 items-center gap-2 rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm font-semibold text-[var(--crm-text)] transition hover:border-cyan-300/35 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <RefreshCw size={16} className={loading ? "animate-spin" : ""} />
              Refresh
            </button>
            <button
              type="button"
              onClick={exportDashboardSummary}
              disabled={loading || !summary}
              className="inline-flex h-10 items-center gap-2 rounded-xl bg-[var(--crm-brand-gradient)] px-3 text-sm font-semibold text-white shadow-[0_14px_34px_rgba(6,74,92,0.22)] transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
            >
              <Download size={16} />
              Export
            </button>
          </div>
        }
      >
        {!visibleWidgets && (
          <GlassCard>
            <p className="text-sm font-semibold text-[var(--crm-text)]">
              Widgets hidden
            </p>
            <p className="mt-2 text-sm text-[var(--crm-text-muted)]">
              Enable dashboard widgets from workspace settings.
            </p>
          </GlassCard>
        )}

        {preferences.widgets.kpis && (
          <motion.section
            className={`grid ${sectionGap} sm:grid-cols-2 xl:grid-cols-6`}
            variants={containerAnimation}
            initial="hidden"
            animate="show"
          >
            <motion.div variants={cardAnimation}>
              <KpiCard
                label="Customers"
                value={loading ? "-" : (summary?.activeCustomers ?? 0)}
                icon={BriefcaseBusiness}
                path="/customers"
                tone="green"
              />
            </motion.div>
            <motion.div variants={cardAnimation}>
              <KpiCard
                label="Leads"
                value={loading ? "-" : (summary?.activeLeads ?? 0)}
                icon={Target}
                path="/leads"
                tone="cyan"
              />
            </motion.div>
            <motion.div variants={cardAnimation}>
              <KpiCard
                label="Open Tasks"
                value={loading ? "-" : (summary?.openTasks ?? 0)}
                icon={ClipboardList}
                path="/tasks"
                tone="amber"
              />
            </motion.div>
            <motion.div variants={cardAnimation}>
              <KpiCard
                label="Done Tasks"
                value={loading ? "-" : (summary?.completedTasks ?? 0)}
                icon={CheckCircle2}
                path="/tasks"
                tone="green"
              />
            </motion.div>
            <motion.div variants={cardAnimation}>
              <KpiCard
                label="Archived"
                value={loading ? "-" : (summary?.archivedCustomers ?? 0)}
                icon={Archive}
                path="/customers"
                tone="red"
              />
            </motion.div>
            <motion.div variants={cardAnimation}>
              <KpiCard
                label="Users"
                value={loading ? "-" : (summary?.totalUsers ?? 0)}
                icon={Users}
                path="/users"
                tone="slate"
              />
            </motion.div>
          </motion.section>
        )}

        {(preferences.widgets.pipeline || preferences.widgets.tasks) && (
          <motion.section
            className={`grid ${sectionGap} xl:grid-cols-[1fr_0.85fr]`}
            variants={containerAnimation}
            initial="hidden"
            animate="show"
          >
            {preferences.widgets.pipeline && (
              <motion.div variants={cardAnimation}>
                <GlassCard className="h-full">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--crm-text-muted)]">
                        Workload
                      </p>
                      <h2 className="mt-2 text-xl font-semibold text-[var(--crm-text)]">
                        Task status
                      </h2>
                    </div>
                    <ClipboardList className="text-cyan-200" size={24} />
                  </div>

                  <div className="mt-6 space-y-3">
                    <ProgressRow
                      label="Open"
                      value={summary?.openTasks ?? 0}
                      max={totalTasks}
                      tone="amber"
                      path="/tasks"
                    />
                    <ProgressRow
                      label="Completed"
                      value={summary?.completedTasks ?? 0}
                      max={totalTasks}
                      tone="green"
                      path="/tasks"
                    />
                  </div>
                </GlassCard>
              </motion.div>
            )}

            {preferences.widgets.tasks && (
              <motion.div variants={cardAnimation}>
                <GlassCard className="h-full">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--crm-text-muted)]">
                        Customers
                      </p>
                      <h2 className="mt-2 text-xl font-semibold text-[var(--crm-text)]">
                        Account status
                      </h2>
                    </div>
                    <BriefcaseBusiness className="text-emerald-200" size={24} />
                  </div>

                  <div className="mt-6 space-y-3">
                    <ProgressRow
                      label="Active"
                      value={summary?.activeCustomers ?? 0}
                      max={totalCustomers}
                      tone="green"
                      path="/customers"
                    />
                    <ProgressRow
                      label="Archived"
                      value={summary?.archivedCustomers ?? 0}
                      max={totalCustomers}
                      tone="red"
                      path="/customers"
                    />
                  </div>
                </GlassCard>
              </motion.div>
            )}
          </motion.section>
        )}

        {(preferences.widgets.distribution || preferences.widgets.focus) && (
          <motion.section
            className={`grid ${sectionGap} xl:grid-cols-[0.95fr_1.05fr]`}
            variants={containerAnimation}
            initial="hidden"
            animate="show"
          >
            {preferences.widgets.distribution && (
              <motion.div variants={cardAnimation}>
                <GlassCard className="h-full">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--crm-text-muted)]">
                        Records
                      </p>
                      <h2 className="mt-2 text-xl font-semibold text-[var(--crm-text)]">
                        CRM totals
                      </h2>
                    </div>
                    <BarChart3 className="text-cyan-200" size={24} />
                  </div>

                  <div className="mt-6 space-y-3">
                    <ActionRow
                      icon={BriefcaseBusiness}
                      label="Active customers"
                      value={summary?.activeCustomers ?? 0}
                      path="/customers"
                    />
                    <ActionRow
                      icon={Target}
                      label="Active leads"
                      value={summary?.activeLeads ?? 0}
                      path="/leads"
                    />
                    <ActionRow
                      icon={Users}
                      label="Team users"
                      value={summary?.totalUsers ?? 0}
                      path="/users"
                    />
                  </div>
                </GlassCard>
              </motion.div>
            )}

            {preferences.widgets.focus && (
              <motion.div variants={cardAnimation}>
                <GlassCard className="h-full">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--crm-text-muted)]">
                        Queue
                      </p>
                      <h2 className="mt-2 text-xl font-semibold text-[var(--crm-text)]">
                        Review list
                      </h2>
                    </div>
                    <ShieldCheck className="text-amber-200" size={24} />
                  </div>

                  <div className="mt-6 space-y-3">
                    <ActionRow
                      icon={ClipboardList}
                      label="Open tasks"
                      value={summary?.openTasks ?? 0}
                      path="/tasks"
                    >
                      Task module
                    </ActionRow>
                    <ActionRow
                      icon={Target}
                      label="Active leads"
                      value={summary?.activeLeads ?? 0}
                      path="/leads"
                    >
                      Lead module
                    </ActionRow>
                    <ActionRow
                      icon={Archive}
                      label="Archived customers"
                      value={summary?.archivedCustomers ?? 0}
                      path="/customers"
                    >
                      Customer module
                    </ActionRow>
                  </div>
                </GlassCard>
              </motion.div>
            )}
          </motion.section>
        )}
      </PageShell>
    </AppLayout>
  );
}
