import { useState, type ReactNode } from 'react'
import { logout as logoutRequest } from '../services/authService'
import {
  BarChart3,
  BriefcaseBusiness,
  ClipboardList,
  Contact,
  FileText,
  LayoutDashboard,
  LogOut,
  Menu,
  NotebookText,
  ShieldCheck,
  Users,
  X,
} from 'lucide-react'

type AppLayoutProps = {
  children: ReactNode
}

const navItems = [
  { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
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
    <div className="min-h-screen bg-slate-100 text-slate-950">
      <button
        onClick={() => setSidebarOpen(true)}
        className="fixed left-4 top-4 z-40 rounded-md border border-slate-200 bg-white p-2 text-slate-700 shadow-sm lg:hidden"
        aria-label="Open navigation"
      >
        <Menu size={20} />
      </button>

      {sidebarOpen && (
        <button
          className="fixed inset-0 z-40 bg-slate-950/50 lg:hidden"
          onClick={() => setSidebarOpen(false)}
          aria-label="Close navigation overlay"
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-50 flex w-72 flex-col bg-slate-950 text-white shadow-xl transition-transform lg:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        } lg:z-30`}
      >
        <div className="flex h-20 items-center justify-between border-b border-white/10 px-5">
          <div>
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-600 text-sm font-bold text-white shadow-lg shadow-blue-950/30">
                CRM
              </div>
              <div>
                <h1 className="text-base font-semibold text-white">Enterprise CRM</h1>
                <p className="text-xs text-slate-400">Sales operations</p>
              </div>
            </div>
          </div>

          <button
            onClick={() => setSidebarOpen(false)}
            className="rounded-md p-2 text-slate-400 hover:bg-white/10 hover:text-white lg:hidden"
            aria-label="Close navigation"
          >
            <X size={18} />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-5">
          <p className="px-3 pb-3 text-xs font-semibold uppercase text-slate-500">
            Workspace
          </p>

          {navItems.map((item) => {
            const Icon = item.icon
            const active = window.location.pathname === item.path

            return (
              <a
                key={item.path}
                href={item.path}
                className={`mb-1 flex h-11 items-center gap-3 rounded-lg px-3 text-sm font-medium transition ${
                  active
                    ? 'bg-blue-600 text-white shadow-lg shadow-blue-950/30'
                    : 'text-slate-300 hover:bg-white/10 hover:text-white'
                }`}
              >
                <Icon size={18} />
                <span>{item.label}</span>
              </a>
            )
          })}
        </nav>

        <div className="border-t border-white/10 p-4">
          <div className="mb-3 rounded-lg bg-white/5 p-3">
            <p className="text-xs font-medium uppercase text-slate-500">Environment</p>
            <p className="mt-1 text-sm font-medium text-slate-200">Local Docker</p>
          </div>

          <button
            onClick={logout}
            className="flex h-11 w-full items-center gap-3 rounded-lg px-3 text-sm font-medium text-slate-300 transition hover:bg-red-500/15 hover:text-red-200"
          >
            <LogOut size={18} />
            Logout
          </button>
        </div>
      </aside>

      <div className="min-h-screen lg:pl-72">
        <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/95 px-6 py-4 shadow-sm backdrop-blur">
          <div className="ml-10 flex flex-col gap-1 lg:ml-0">
            <p className="text-xs font-semibold uppercase text-blue-700">CRM Workspace</p>
            <h2 className="text-xl font-semibold text-slate-950">{pageTitle}</h2>
          </div>
        </header>

        <main className="mx-auto w-full max-w-7xl p-4 sm:p-6">{children}</main>
      </div>
    </div>
  )
}
