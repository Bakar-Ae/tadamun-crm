import { useState, type ReactNode } from "react";
import { motion } from "framer-motion";
import { Link, useLocation, useNavigate } from "react-router";
import { logout as logoutRequest } from "../services/authService";
import { cn } from "../lib/cn";
import { ThemeToggle } from "../components/ui";
import { CommandMenu } from "../components/CommandMenu";
import { NotificationPanel } from "../components/NotificationPanel";
import { QuickCreateMenu } from "../components/QuickCreateMenu";
import { SettingsPanel } from "../components/SettingsPanel";
import { UserMenu } from "../components/UserMenu";
import tadamunLogo from "../assets/tadamun-logo.svg";
import {
  Bell,
  BarChart3,
  BriefcaseBusiness,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Contact,
  FileText,
  KeyRound,
  LayoutDashboard,
  Menu,
  NotebookText,
  ShieldCheck,
  Sparkles,
  Users,
  X,
} from "lucide-react";

type AppLayoutProps = {
  children: ReactNode;
};

const navItems = [
  { label: "Dashboard", path: "/dashboard", icon: LayoutDashboard },
  { label: "Notifications", path: "/notifications", icon: Bell },
  { label: "Users", path: "/users", icon: Users },
  { label: "Customers", path: "/customers", icon: BriefcaseBusiness },
  { label: "Leads", path: "/leads", icon: ClipboardList },
  { label: "Contacts", path: "/contacts", icon: Contact },
  { label: "Tasks", path: "/tasks", icon: NotebookText },
  { label: "Notes", path: "/notes", icon: FileText },
  { label: "Reports", path: "/reports", icon: BarChart3 },
  { label: "Audit Logs", path: "/audit-logs", icon: ShieldCheck },
  { label: "Change Password", path: "/change-password", icon: KeyRound },
];

const favoritePaths = new Set(["/dashboard", "/customers", "/tasks"]);

export function AppLayout({ children }: AppLayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [sidebarCompact, setSidebarCompact] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const pageTitle =
    navItems.find((item) => item.path === location.pathname)?.label ??
    "Dashboard";

  async function logout() {
    const refreshToken = localStorage.getItem("refreshToken");

    try {
      if (refreshToken) {
        await logoutRequest(refreshToken);
      }
    } finally {
      localStorage.removeItem("token");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("user");
      navigate("/", { replace: true });
    }
  }

  return (
    <div className="min-h-screen overflow-hidden bg-[var(--crm-bg)] text-[var(--crm-text)]">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_12%_4%,rgba(109,93,251,0.16),transparent_30rem),radial-gradient(circle_at_86%_0%,rgba(56,189,248,0.12),transparent_24rem)]" />

      <button
        onClick={() => setSidebarOpen(true)}
        className="fixed left-4 top-4 z-40 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface-glass)] p-2 text-[var(--crm-text)] shadow-lg backdrop-blur lg:hidden"
        aria-label="Open navigation"
      >
        <Menu size={20} />
      </button>

      {sidebarOpen && (
        <button
          className="fixed inset-0 z-40 bg-black/70 backdrop-blur-sm lg:hidden"
          onClick={() => setSidebarOpen(false)}
          aria-label="Close navigation overlay"
        />
      )}

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-50 flex w-72 flex-col border-r border-[var(--crm-border)] bg-[var(--crm-surface-glass)] text-[var(--crm-text)] shadow-[var(--crm-shadow-card)] backdrop-blur-xl transition-all lg:translate-x-0",
          sidebarCompact ? "lg:w-20" : "lg:w-72",
          sidebarOpen ? "translate-x-0" : "-translate-x-full",
          "lg:z-30",
        )}
      >
        <div className="flex h-20 items-center justify-between border-b border-[var(--crm-border)] px-4">
          <Link
            to="/dashboard"
            className="flex min-w-0 items-center gap-3"
            onClick={() => setSidebarOpen(false)}
          >
            <div className="relative flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-white ring-1 ring-violet-200 shadow-[0_16px_36px_rgba(109,93,251,0.16)]">
              <img
                src={tadamunLogo}
                alt="Tadamun logo"
                className="h-10 w-10"
              />
              <span className="absolute -right-1 -top-1 h-3 w-3 rounded-full bg-[var(--crm-success)] shadow-[0_0_16px_rgba(16,185,129,0.55)]" />
            </div>
            {!sidebarCompact && (
              <div className="min-w-0">
                <h1 className="truncate text-base font-semibold text-[var(--crm-text)]">
                  Tadamun
                </h1>
                <p className="truncate text-xs text-[var(--crm-text-muted)]">
                  Business Solutions CRM
                </p>
              </div>
            )}
          </Link>

          <div className="flex items-center gap-1">
            <button
              onClick={() => setSidebarCompact((value) => !value)}
              className="hidden rounded-xl p-2 text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-primary)] lg:grid"
              aria-label={
                sidebarCompact ? "Expand navigation" : "Collapse navigation"
              }
            >
              {sidebarCompact ? (
                <ChevronRight size={17} />
              ) : (
                <ChevronLeft size={17} />
              )}
            </button>
            <button
              onClick={() => setSidebarOpen(false)}
              className="rounded-xl p-2 text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-primary)] lg:hidden"
              aria-label="Close navigation"
            >
              <X size={18} />
            </button>
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-5">
          {!sidebarCompact && (
            <p className="px-3 pb-3 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--crm-text-muted)]">
              Favorites
            </p>
          )}

          {navItems
            .filter((item) => favoritePaths.has(item.path))
            .map((item) => {
              const Icon = item.icon;
              const active = location.pathname === item.path;

              return (
                <Link
                  key={`favorite-${item.path}`}
                  to={item.path}
                  onClick={() => setSidebarOpen(false)}
                  title={item.label}
                  className={cn(
                    "group relative mb-1 flex h-11 items-center gap-3 overflow-hidden rounded-xl px-3 text-sm font-medium transition",
                    sidebarCompact && "justify-center px-0",
                    active
                      ? "bg-[var(--crm-primary)] text-white shadow-[0_14px_34px_rgba(109,93,251,0.24)]"
                      : "text-[var(--crm-text-muted)] hover:bg-violet-500/10 hover:text-[var(--crm-text)]",
                  )}
                >
                  {active && (
                    <motion.span
                      layoutId="active-nav"
                      className="absolute inset-y-1 left-1 w-1 rounded-full bg-white/80"
                    />
                  )}
                  <Icon
                    size={18}
                    className={cn(
                      "relative z-10 transition",
                      active
                        ? "text-white"
                        : "text-[var(--crm-text-muted)] group-hover:text-[var(--crm-primary)]",
                    )}
                  />
                  {!sidebarCompact && (
                    <span className="relative z-10">{item.label}</span>
                  )}
                </Link>
              );
            })}

          {!sidebarCompact && (
            <p className="px-3 pb-3 pt-5 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--crm-text-muted)]">
              Workspace
            </p>
          )}

          {navItems
            .filter((item) => !favoritePaths.has(item.path))
            .map((item) => {
              const Icon = item.icon;
              const active = location.pathname === item.path;

              return (
                <Link
                  key={item.path}
                  to={item.path}
                  onClick={() => setSidebarOpen(false)}
                  title={item.label}
                  className={cn(
                    "group relative mb-1 flex h-11 items-center gap-3 overflow-hidden rounded-xl px-3 text-sm font-medium transition",
                    sidebarCompact && "justify-center px-0",
                    active
                      ? "bg-[var(--crm-primary)] text-white shadow-[0_14px_34px_rgba(109,93,251,0.24)]"
                      : "text-[var(--crm-text-muted)] hover:bg-violet-500/10 hover:text-[var(--crm-text)]",
                  )}
                >
                  {active && (
                    <motion.span
                      layoutId="active-nav"
                      className="absolute inset-y-1 left-1 w-1 rounded-full bg-white/80"
                    />
                  )}
                  <Icon
                    size={18}
                    className={cn(
                      "relative z-10 transition",
                      active
                        ? "text-white"
                        : "text-[var(--crm-text-muted)] group-hover:text-[var(--crm-primary)]",
                    )}
                  />
                  {!sidebarCompact && (
                    <span className="relative z-10">{item.label}</span>
                  )}
                </Link>
              );
            })}
        </nav>

        {!sidebarCompact && (
          <div className="border-t border-[var(--crm-border)] p-4">
            <div className="rounded-3xl border border-[var(--crm-border)] bg-[var(--crm-soft-gradient)] p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-white/60 text-[var(--crm-primary)] ring-1 ring-violet-200">
                  <Sparkles size={18} />
                </div>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[var(--crm-text-muted)]">
                    Environment
                  </p>
                  <p className="mt-1 text-sm font-medium text-[var(--crm-text)]">
                    Local Docker
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}
      </aside>

      <div
        className={cn(
          "relative z-10 min-h-screen transition-all",
          sidebarCompact ? "lg:pl-20" : "lg:pl-72",
        )}
      >
        <header className="sticky top-0 z-20 border-b border-[var(--crm-border)] bg-[var(--crm-surface-glass)] px-4 py-4 shadow-sm backdrop-blur-xl sm:px-6">
          <div className="ml-10 flex items-center justify-between gap-4 lg:ml-0">
            <div className="min-w-0">
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--crm-accent-text)]">
                Tadamun Business Suite
              </p>
              <h2 className="truncate text-xl font-semibold text-[var(--crm-text)]">
                {pageTitle}
              </h2>
            </div>

            <div className="flex shrink-0 items-center gap-2">
              <CommandMenu />
              <QuickCreateMenu />
              <NotificationPanel />
              <SettingsPanel />
              <ThemeToggle />
              <UserMenu onLogout={logout} />
            </div>
          </div>
        </header>

        <main id="main-content" className="mx-auto w-full max-w-7xl p-4 sm:p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
