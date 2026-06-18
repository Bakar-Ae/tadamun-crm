import { useState, type ReactNode } from 'react'
import { motion } from 'framer-motion'
import { logout as logoutRequest } from '../services/authService'
import { cn } from '../lib/cn'
import {
  Bell,
  BarChart3,
  BriefcaseBusiness,
  ClipboardList,
  Contact,
  FileText,
  KeyRound,
  LayoutDashboard,
  LogOut,
  Menu,
  NotebookText,
  ShieldCheck,
  Sparkles,
  Users,
  X,
} from 'lucide-react'

type AppLayoutProps = {
  children: ReactNode
}

const navItems = [
  { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
  { label: 'Notifications', path: '/notifications', icon: Bell },
  { label: 'Change Password', path: '/change-password', icon: KeyRound },
  { label: 'Users', path: '/users', icon: Users },
  { label: 'Customers', path: '/customers', icon: BriefcaseBusiness },
  { label: 'Leads', path: '/leads', icon: ClipboardList },
  { label: 'Contacts', path: '/contacts', icon: Contact },
  { label: 'Tasks', path: '/tasks', icon: NotebookText },
  { label: 'Notes', path: '/notes', icon: FileText },
  { label: 'Reports', path: '/reports', icon: BarChart3 },
  { label: 'Audit Logs', path: '/audit-logs', icon: ShieldCheck },
]

function getPageTitle() {
  const current = navItems.find((item) => item.path === window.location.pathname)
  return current?.label ?? 'Dashboard'
}

export function AppLayout({ children }: AppLayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const pageTitle = getPageTitle()

  async function logout() {
    const refreshToken = localStorage.getItem('refreshToken')

    try {
      if (refreshToken) {
        await logoutRequest(refreshToken)
      }
    } finally {
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      window.location.href = '/'
    }
  }

  return (
    <div className="min-h-screen overflow-hidden bg-[var(--crm-bg)] text-white">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_top_left,rgba(65,192,242,0.16),transparent_28rem),radial-gradient(circle_at_80%_0%,rgba(2,245,161,0.08),transparent_24rem)]" />
      <div className="pointer-events-none fixed inset-0 bg-[linear-gradient(to_right,rgba(173,223,241,0.04)_1px,transparent_1px),linear-gradient(to_bottom,rgba(173,223,241,0.04)_1px,transparent_1px)] bg-[size:48px_48px]" />

      <button
        onClick={() => setSidebarOpen(true)}
        className="fixed left-4 top-4 z-40 rounded-xl border border-white/10 bg-white/10 p-2 text-white shadow-lg backdrop-blur lg:hidden"
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
          'fixed inset-y-0 left-0 z-50 flex w-72 flex-col border-r border-white/10 bg-slate-950/85 text-white shadow-2xl shadow-black/40 backdrop-blur-xl transition-transform lg:translate-x-0',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full',
          'lg:z-30',
        )}
      >
        <div className="flex h-20 items-center justify-between border-b border-white/10 px-5">
          <div className="flex items-center gap-3">
            <div className="relative flex h-11 w-11 items-center justify-center rounded-2xl bg-cyan-400/15 text-sm font-bold text-cyan-100 ring-1 ring-cyan-300/25 shadow-[0_0_30px_rgba(65,192,242,0.18)]">
              CRM
              <span className="absolute -right-1 -top-1 h-3 w-3 rounded-full bg-emerald-400 shadow-[0_0_16px_rgba(2,245,161,0.9)]" />
            </div>
            <div>
              <h1 className="text-base font-semibold text-white">Enterprise CRM</h1>
              <p className="text-xs text-slate-400">Revenue command center</p>
            </div>
          </div>

          <button
            onClick={() => setSidebarOpen(false)}
            className="rounded-lg p-2 text-slate-400 transition hover:bg-white/10 hover:text-white lg:hidden"
            aria-label="Close navigation"
          >
            <X size={18} />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-5">
          <p className="px-3 pb-3 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
            Workspace
          </p>

          {navItems.map((item) => {
            const Icon = item.icon
            const active = window.location.pathname === item.path

            return (
              <a
                key={item.path}
                href={item.path}
                className={cn(
                  'group relative mb-1 flex h-11 items-center gap-3 overflow-hidden rounded-xl px-3 text-sm font-medium transition',
                  active
                    ? 'bg-cyan-400/15 text-white ring-1 ring-cyan-300/20 shadow-[0_0_28px_rgba(65,192,242,0.12)]'
                    : 'text-slate-400 hover:bg-white/8 hover:text-white',
                )}
              >
                {active && (
                  <motion.span
                    layoutId="active-nav"
                    className="absolute inset-y-1 left-1 w-1 rounded-full bg-cyan-300"
                  />
                )}
                <Icon
                  size={18}
                  className={cn(
                    'relative z-10 transition',
                    active ? 'text-cyan-200' : 'text-slate-500 group-hover:text-cyan-200',
                  )}
                />
                <span className="relative z-10">{item.label}</span>
              </a>
            )
          })}
        </nav>

        <div className="border-t border-white/10 p-4">
          <div className="mb-3 rounded-2xl border border-white/10 bg-white/[0.06] p-4">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-400/10 text-emerald-200 ring-1 ring-emerald-300/20">
                <Sparkles size={18} />
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
                  Environment
                </p>
                <p className="mt-1 text-sm font-medium text-slate-100">Local Docker</p>
              </div>
            </div>
          </div>

          <button
            onClick={logout}
            className="flex h-11 w-full items-center gap-3 rounded-xl px-3 text-sm font-medium text-slate-400 transition hover:bg-red-500/15 hover:text-red-200"
          >
            <LogOut size={18} />
            Logout
          </button>
        </div>
      </aside>

      <div className="relative z-10 min-h-screen lg:pl-72">
        <header className="sticky top-0 z-20 border-b border-white/10 bg-slate-950/70 px-6 py-4 shadow-lg shadow-black/20 backdrop-blur-xl">
          <div className="ml-10 flex flex-col gap-1 lg:ml-0">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-cyan-300">
              CRM Workspace
            </p>
            <h2 className="text-xl font-semibold text-white">{pageTitle}</h2>
          </div>
        </header>

        <main className="mx-auto w-full max-w-7xl p-4 sm:p-6">{children}</main>
      </div>
    </div>
  )
}