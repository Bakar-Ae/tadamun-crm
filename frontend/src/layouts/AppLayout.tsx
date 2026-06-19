import { useState, type ReactNode } from 'react'
import {
  BarChart3,
  BriefcaseBusiness,
  ClipboardList,
  Contact,
  FileText,
  LayoutDashboard,
  Menu,
  NotebookText,
  ShieldCheck,
  Users,
  X,
} from 'lucide-react'
import { NavLink, useLocation, useNavigate } from 'react-router'
import { CommandMenu } from '../components/CommandMenu'
import { NotificationPanel } from '../components/NotificationPanel'
import { QuickCreateMenu } from '../components/QuickCreateMenu'
import { SettingsPanel } from '../components/SettingsPanel'
import { UserMenu } from '../components/UserMenu'
import { logout as logoutRequest } from '../services/authService'
import tadamunLogo from '../assets/tadamun-logo.svg'

type AppLayoutProps = {
  children: ReactNode
}

type NavItem = {
  label: string
  path: string
  icon: typeof LayoutDashboard
  description: string
}

type NavGroup = {
  title: string
  items: NavItem[]
}

const navGroups: NavGroup[] = [
  {
    title: 'Overview',
    items: [
      {
        label: 'Dashboard',
        path: '/dashboard',
        icon: LayoutDashboard,
        description: "Today's CRM overview",
      },
    ],
  },
  {
    title: 'CRM',
    items: [
      {
        label: 'Customers',
        path: '/customers',
        icon: BriefcaseBusiness,
        description: 'Customer accounts and profiles',
      },
      {
        label: 'Leads',
        path: '/leads',
        icon: ClipboardList,
        description: 'Sales opportunities and status',
      },
      {
        label: 'Contacts',
        path: '/contacts',
        icon: Contact,
        description: 'People connected to customers',
      },
      {
        label: 'Tasks',
        path: '/tasks',
        icon: NotebookText,
        description: 'Assigned work and follow-ups',
      },
      {
        label: 'Notes',
        path: '/notes',
        icon: FileText,
        description: 'Customer and lead notes',
      },
    ],
  },
  {
    title: 'Admin',
    items: [
      {
        label: 'Users',
        path: '/users',
        icon: Users,
        description: 'Team accounts and access',
      },
      {
        label: 'Reports',
        path: '/reports',
        icon: BarChart3,
        description: 'CRM totals and reports',
      },
      {
        label: 'Audit Logs',
        path: '/audit-logs',
        icon: ShieldCheck,
        description: 'Security activity history',
      },
    ],
  },
]

const allNavItems = navGroups.flatMap((group) => group.items)

function getCurrentNavItem(pathname: string) {
  return allNavItems.find((item) => item.path === pathname) ?? allNavItems[0]
}

export function AppLayout({ children }: AppLayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const currentNav = getCurrentNavItem(location.pathname)

  async function logout() {
    const refreshToken = localStorage.getItem('refreshToken')

    if (refreshToken) {
      await logoutRequest(refreshToken).catch(() => undefined)
    }

    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    navigate('/', { replace: true })
  }

  return (
    <div className="min-h-screen bg-[var(--crm-bg)] text-[var(--crm-text)]">
      {sidebarOpen && (
        <button
          type="button"
          className="fixed inset-0 z-40 bg-slate-950/40 backdrop-blur-[2px] lg:hidden"
          onClick={() => setSidebarOpen(false)}
          aria-label="Close navigation overlay"
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-50 flex w-72 flex-col border-r border-[var(--crm-border)] bg-[var(--crm-surface-glass)] text-[var(--crm-text)] shadow-[var(--crm-shadow-soft)] backdrop-blur-xl transition-transform duration-200 ease-out lg:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        } lg:z-30`}
      >
        <div className="flex h-20 items-center justify-between border-b border-[var(--crm-border)] px-4">
          <NavLink
            to="/dashboard"
            onClick={() => setSidebarOpen(false)}
            className="flex min-w-0 items-center gap-3"
          >
            <span className="grid h-12 w-12 shrink-0 place-items-center overflow-hidden rounded-2xl bg-white shadow-[0_14px_34px_rgba(109,93,251,0.16)] ring-1 ring-violet-200">
              <img src={tadamunLogo} alt="Tadamun" className="h-10 w-10" />
            </span>
            <span className="min-w-0">
              <span className="block truncate text-base font-semibold text-[var(--crm-text)]">
                Tadamun
              </span>
              <span className="block truncate text-xs font-medium text-[var(--crm-text-muted)]">
                Business Solutions
              </span>
            </span>
          </NavLink>

          <button
            type="button"
            onClick={() => setSidebarOpen(false)}
            className="grid h-10 w-10 place-items-center rounded-2xl text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-text)] lg:hidden"
            aria-label="Close navigation"
          >
            <X size={18} />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-5">
          {navGroups.map((group) => (
            <section key={group.title} className="mb-6 last:mb-0">
              <p className="mb-2 px-3 text-[11px] font-semibold uppercase tracking-[0.18em] text-[var(--crm-text-muted)]">
                {group.title}
              </p>

              <div className="flex flex-col gap-1">
                {group.items.map((item) => {
                  const Icon = item.icon

                  return (
                    <NavLink
                      key={item.path}
                      to={item.path}
                      onClick={() => setSidebarOpen(false)}
                      className={({ isActive }) =>
                        `group flex min-h-11 items-center gap-3 rounded-2xl px-3 text-sm font-semibold transition ${
                          isActive
                            ? 'bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)] ring-1 ring-violet-300/25'
                            : 'text-[var(--crm-text-muted)] hover:bg-violet-500/10 hover:text-[var(--crm-text)]'
                        }`
                      }
                    >
                      {({ isActive }) => (
                        <>
                          <span
                            className={`grid h-8 w-8 shrink-0 place-items-center rounded-xl transition ${
                              isActive
                                ? 'bg-white/70 text-[var(--crm-primary)] shadow-sm'
                                : 'bg-[var(--crm-surface-soft)] text-[var(--crm-text-muted)] group-hover:text-[var(--crm-primary)]'
                            }`}
                          >
                            <Icon size={17} strokeWidth={isActive ? 2.3 : 2} />
                          </span>
                          <span className="truncate">{item.label}</span>
                        </>
                      )}
                    </NavLink>
                  )
                })}
              </div>
            </section>
          ))}
        </nav>
      </aside>

      <div className="min-h-screen lg:pl-72">
        <header className="sticky top-0 z-20 border-b border-[var(--crm-border)] bg-[var(--crm-surface-glass)] backdrop-blur-xl">
          <div className="flex min-h-16 items-center gap-3 px-4 py-3 sm:px-6">
            <button
              type="button"
              onClick={() => setSidebarOpen(true)}
              className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] text-[var(--crm-text-muted)] shadow-sm transition hover:border-violet-300 hover:text-[var(--crm-text)] lg:hidden"
              aria-label="Open navigation"
            >
              <Menu size={18} />
            </button>

            <div className="min-w-0 flex-1">
              <h2 className="truncate text-lg font-semibold text-[var(--crm-text)] sm:text-xl">
                {currentNav.label}
              </h2>
              <p className="mt-0.5 hidden truncate text-sm text-[var(--crm-text-muted)] sm:block">
                {currentNav.description}
              </p>
            </div>

            <div className="flex shrink-0 items-center gap-2">
              <CommandMenu />
              <QuickCreateMenu />
              <NotificationPanel />
              <SettingsPanel />
              <UserMenu onLogout={logout} />
            </div>
          </div>
        </header>

        <main className="mx-auto w-full max-w-7xl p-4 sm:p-6">{children}</main>
      </div>
    </div>
  )
}
