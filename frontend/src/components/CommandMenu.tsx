import { useEffect, useState } from 'react'
import { Command } from 'cmdk'
import { AnimatePresence, motion } from 'framer-motion'
import {
  Bell,
  BriefcaseBusiness,
  Building2,
  ClipboardList,
  Contact,
  FileText,
  KeyRound,
  LayoutDashboard,
  LoaderCircle,
  NotebookText,
  Search,
  ShieldCheck,
  X,
} from 'lucide-react'
import { useNavigate } from 'react-router'
import { formatStatus } from '../lib/formatters'
import { getGlobalSearchResultPath } from '../lib/globalSearch'
import {
  searchWorkspace,
  type GlobalSearchResult,
  type SearchModule,
} from '../services/globalSearchService'
import type { PermissionName } from '../services/permissionService'
import { useWorkspace } from '../workspace/useWorkspace'

type CommandItem = {
  label: string
  path: string
  icon: typeof LayoutDashboard
  requiredPermission?: PermissionName
}

const commandItems: CommandItem[] = [
  {
    label: 'Dashboard',
    path: '/dashboard',
    icon: LayoutDashboard,
    requiredPermission: 'DASHBOARD_VIEW',
  },
  { label: 'Notifications', path: '/notifications', icon: Bell },
  {
    label: 'Organization',
    path: '/organization',
    icon: Building2,
    requiredPermission: 'ORGANIZATION_VIEW',
  },
  {
    label: 'Customers',
    path: '/customers',
    icon: BriefcaseBusiness,
    requiredPermission: 'CUSTOMER_VIEW',
  },
  { label: 'Leads', path: '/leads', icon: ClipboardList, requiredPermission: 'LEAD_VIEW' },
  { label: 'Contacts', path: '/contacts', icon: Contact, requiredPermission: 'CONTACT_VIEW' },
  { label: 'Tasks', path: '/tasks', icon: NotebookText, requiredPermission: 'TASK_VIEW' },
  { label: 'Notes', path: '/notes', icon: FileText, requiredPermission: 'NOTE_VIEW' },
  { label: 'Reports', path: '/reports', icon: LayoutDashboard, requiredPermission: 'REPORT_VIEW' },
  {
    label: 'Audit Logs',
    path: '/audit-logs',
    icon: ShieldCheck,
    requiredPermission: 'AUDIT_LOG_VIEW',
  },
  { label: 'Account Security', path: '/change-password', icon: KeyRound },
]

const searchModules: Array<{
  value: SearchModule
  label: string
  icon: typeof BriefcaseBusiness
  requiredPermission: PermissionName
}> = [
  { value: 'CUSTOMER', label: 'Customers', icon: BriefcaseBusiness, requiredPermission: 'CUSTOMER_VIEW' },
  { value: 'LEAD', label: 'Leads', icon: ClipboardList, requiredPermission: 'LEAD_VIEW' },
  { value: 'CONTACT', label: 'Contacts', icon: Contact, requiredPermission: 'CONTACT_VIEW' },
  { value: 'TASK', label: 'Tasks', icon: NotebookText, requiredPermission: 'TASK_VIEW' },
  { value: 'NOTE', label: 'Notes', icon: FileText, requiredPermission: 'NOTE_VIEW' },
]

export function CommandMenu() {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [activeModule, setActiveModule] = useState<SearchModule | null>(null)
  const [results, setResults] = useState<GlobalSearchResult[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const { activeWorkspace } = useWorkspace()
  const permissions = new Set(activeWorkspace?.permissions ?? [])
  const availableCommandItems = commandItems.filter(
    (item) => !item.requiredPermission || permissions.has(item.requiredPermission),
  )
  const availableSearchModules = searchModules.filter((module) =>
    permissions.has(module.requiredPermission),
  )
  const normalizedQuery = query.trim()
  const hasSearchQuery = normalizedQuery.length >= 2

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        setOpen((value) => !value)
      }

      if (event.key === 'Escape') {
        setOpen(false)
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  useEffect(() => {
    if (!open || normalizedQuery.length < 2) {
      return
    }

    const controller = new AbortController()
    const timeoutId = window.setTimeout(() => {
      setLoading(true)
      setError('')

      searchWorkspace(normalizedQuery, activeModule, controller.signal)
        .then((response) => {
          if (!controller.signal.aborted) {
            setResults(response.results)
          }
        })
        .catch(() => {
          if (!controller.signal.aborted) {
            setResults([])
            setError('Search is unavailable. Please try again.')
          }
        })
        .finally(() => {
          if (!controller.signal.aborted) {
            setLoading(false)
          }
        })
    }, 250)

    return () => {
      window.clearTimeout(timeoutId)
      controller.abort()
    }
  }, [activeModule, normalizedQuery, open])

  function closeMenu() {
    setOpen(false)
    setQuery('')
    setActiveModule(null)
    setResults([])
    setLoading(false)
    setError('')
  }

  function runCommand(path: string) {
    navigate(path)
    closeMenu()
  }

  function handleQueryChange(value: string) {
    setQuery(value)

    if (value.trim().length < 2) {
      setResults([])
      setLoading(false)
      setError('')
    }
  }

  function handleModuleChange(module: SearchModule | null) {
    setActiveModule(module)
    setResults([])
  }

  const matchingCommands = availableCommandItems.filter((item) =>
    item.label.toLowerCase().includes(normalizedQuery.toLowerCase()),
  )

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="hidden h-11 min-w-56 items-center justify-between rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text-muted)] shadow-sm transition hover:border-violet-300 hover:text-[var(--crm-text)] md:inline-flex"
      >
        <span className="inline-flex items-center gap-2">
          <Search size={16} />
          Search workspace
        </span>
        <kbd className="rounded-md border border-[var(--crm-border)] px-1.5 py-0.5 text-[10px] font-semibold">
          Ctrl K
        </kbd>
      </button>

      <button
        type="button"
        onClick={() => setOpen(true)}
        className="grid h-11 w-11 place-items-center rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] text-[var(--crm-text-muted)] shadow-sm transition hover:border-violet-300 hover:text-[var(--crm-text)] md:hidden"
        aria-label="Open workspace search"
      >
        <Search size={17} />
      </button>

      <AnimatePresence>
        {open && (
          <div className="fixed inset-0 z-[90] flex items-start justify-center p-4 pt-[10vh]">
            <motion.button
              type="button"
              className="absolute inset-0 bg-black/70 backdrop-blur-sm"
              aria-label="Close workspace search"
              onClick={closeMenu}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            />

            <motion.div
              role="dialog"
              aria-modal="true"
              aria-label="Search Tadamun CRM"
              className="relative w-full max-w-2xl overflow-hidden rounded-[2rem] border border-[var(--crm-border)] bg-[var(--crm-surface)] text-[var(--crm-text)] shadow-[var(--crm-shadow-soft)]"
              initial={{ opacity: 0, y: 18, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 14, scale: 0.98 }}
              transition={{ duration: 0.18, ease: 'easeOut' }}
            >
              <Command label="Tadamun workspace search" shouldFilter={false}>
                <div className="flex items-center gap-3 border-b border-[var(--crm-border)] px-4">
                  <Search size={18} className="text-[var(--crm-text-muted)]" />
                  <Command.Input
                    autoFocus
                    value={query}
                    onValueChange={handleQueryChange}
                    placeholder="Search customers, leads, contacts, tasks, and notes"
                    className="h-14 flex-1 bg-transparent text-sm outline-none placeholder:text-[var(--crm-text-muted)]"
                  />
                  <button
                    type="button"
                    onClick={closeMenu}
                    className="grid h-11 w-11 place-items-center rounded-xl text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-primary)]"
                    aria-label="Close workspace search"
                  >
                    <X size={17} />
                  </button>
                </div>

                {hasSearchQuery && (
                  <div
                    className="flex gap-2 overflow-x-auto border-b border-[var(--crm-border)] px-4 py-3"
                    aria-label="Filter search results by record type"
                  >
                    <button
                      type="button"
                      onClick={() => handleModuleChange(null)}
                      className={`h-10 shrink-0 rounded-xl px-3 text-xs font-semibold transition ${
                        activeModule === null
                          ? 'bg-[var(--crm-primary)] text-white'
                          : 'bg-[var(--crm-card-subtle)] text-[var(--crm-text-muted)] hover:text-[var(--crm-text)]'
                      }`}
                    >
                      All records
                    </button>
                    {availableSearchModules.map((module) => (
                      <button
                        key={module.value}
                        type="button"
                        onClick={() => handleModuleChange(module.value)}
                        className={`h-10 shrink-0 rounded-xl px-3 text-xs font-semibold transition ${
                          activeModule === module.value
                            ? 'bg-[var(--crm-primary)] text-white'
                            : 'bg-[var(--crm-card-subtle)] text-[var(--crm-text-muted)] hover:text-[var(--crm-text)]'
                        }`}
                      >
                        {module.label}
                      </button>
                    ))}
                  </div>
                )}

                <Command.List className="max-h-[min(65vh,560px)] overflow-y-auto p-3">
                  {loading && (
                    <div
                      className="flex items-center justify-center gap-2 px-3 py-8 text-sm text-[var(--crm-text-muted)]"
                      role="status"
                    >
                      <LoaderCircle size={18} className="animate-spin" />
                      Searching records...
                    </div>
                  )}

                  {!loading && error && (
                    <div
                      className="rounded-2xl border border-red-300/40 bg-red-500/10 px-4 py-4 text-sm text-red-600"
                      role="alert"
                    >
                      {error}
                    </div>
                  )}

                  {!loading && !error && hasSearchQuery && results.length === 0 && (
                    <div className="px-3 py-8 text-center">
                      <p className="font-semibold text-[var(--crm-text)]">No CRM records found</p>
                      <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
                        Check the spelling or try another record type.
                      </p>
                    </div>
                  )}

                  {!loading &&
                    !error &&
                    availableSearchModules.map((module) => {
                      const moduleResults = results.filter(
                        (result) => result.module === module.value,
                      )

                      if (moduleResults.length === 0) {
                        return null
                      }

                      const Icon = module.icon

                      return (
                        <Command.Group
                          key={module.value}
                          heading={module.label}
                          className="mb-3 text-xs font-semibold uppercase tracking-[0.16em] text-[var(--crm-text-muted)]"
                        >
                          {moduleResults.map((result) => (
                            <Command.Item
                              key={`${result.module}-${result.id}`}
                              value={`${result.module}-${result.id}`}
                              onSelect={() => runCommand(getGlobalSearchResultPath(result))}
                              className="mt-1 flex min-h-14 cursor-pointer items-center gap-3 rounded-2xl px-3 py-2 text-left text-sm text-[var(--crm-text)] outline-none aria-selected:bg-violet-500/10"
                            >
                              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-violet-500/10 text-[var(--crm-primary)] ring-1 ring-violet-300/20">
                                <Icon size={17} />
                              </span>
                              <span className="min-w-0 flex-1">
                                <span className="block truncate font-semibold">{result.title}</span>
                                {result.description && (
                                  <span className="mt-0.5 block truncate text-xs font-normal normal-case tracking-normal text-[var(--crm-text-muted)]">
                                    {result.description}
                                  </span>
                                )}
                              </span>
                              {result.status && (
                                <span className="shrink-0 rounded-full border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] px-2 py-1 text-[10px] font-semibold normal-case tracking-normal text-[var(--crm-text-muted)]">
                                  {formatStatus(result.status)}
                                </span>
                              )}
                            </Command.Item>
                          ))}
                        </Command.Group>
                      )
                    })}

                  {matchingCommands.length > 0 && (
                    <Command.Group
                      heading="Pages"
                      className="text-xs font-semibold uppercase tracking-[0.16em] text-[var(--crm-text-muted)]"
                    >
                      {matchingCommands.map((item) => {
                        const Icon = item.icon

                        return (
                          <Command.Item
                            key={item.path}
                            value={`page-${item.label}`}
                            onSelect={() => runCommand(item.path)}
                            className="mt-1 flex min-h-14 cursor-pointer items-center gap-3 rounded-2xl px-3 py-2 text-sm text-[var(--crm-text)] outline-none aria-selected:bg-violet-500/10"
                          >
                            <span className="grid h-10 w-10 place-items-center rounded-xl bg-violet-500/10 text-[var(--crm-primary)] ring-1 ring-violet-300/20">
                              <Icon size={17} />
                            </span>
                            {item.label}
                          </Command.Item>
                        )
                      })}
                    </Command.Group>
                  )}

                  {!hasSearchQuery && (
                    <p className="px-3 pb-2 pt-3 text-xs font-normal normal-case tracking-normal text-[var(--crm-text-muted)]">
                      Type at least two characters to search CRM records.
                    </p>
                  )}
                </Command.List>
              </Command>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </>
  )
}
