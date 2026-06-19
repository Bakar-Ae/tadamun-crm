import { useEffect, useState } from "react";
import { Command } from "cmdk";
import { AnimatePresence, motion } from "framer-motion";
import {
  Bell,
  BriefcaseBusiness,
  ClipboardList,
  Contact,
  FileText,
  KeyRound,
  LayoutDashboard,
  NotebookText,
  Search,
  ShieldCheck,
  Users,
  X,
} from "lucide-react";
import { useNavigate } from "react-router";

const commandItems = [
  { label: "Dashboard", path: "/dashboard", icon: LayoutDashboard },
  { label: "Notifications", path: "/notifications", icon: Bell },
  { label: "Users", path: "/users", icon: Users },
  { label: "Customers", path: "/customers", icon: BriefcaseBusiness },
  { label: "Leads", path: "/leads", icon: ClipboardList },
  { label: "Contacts", path: "/contacts", icon: Contact },
  { label: "Tasks", path: "/tasks", icon: NotebookText },
  { label: "Notes", path: "/notes", icon: FileText },
  { label: "Reports", path: "/reports", icon: LayoutDashboard },
  { label: "Audit Logs", path: "/audit-logs", icon: ShieldCheck },
  { label: "Account Security", path: "/change-password", icon: KeyRound },
];

export function CommandMenu() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setOpen((value) => !value);
      }
    }

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  function runCommand(path: string) {
    navigate(path);
    setOpen(false);
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="hidden h-10 min-w-56 items-center justify-between rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text-muted)] shadow-sm transition hover:border-violet-300 hover:text-[var(--crm-text)] md:inline-flex"
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
        className="grid h-10 w-10 place-items-center rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] text-[var(--crm-text-muted)] shadow-sm transition hover:border-violet-300 hover:text-[var(--crm-text)] md:hidden"
        aria-label="Open command menu"
      >
        <Search size={17} />
      </button>

      <AnimatePresence>
        {open && (
          <div className="fixed inset-0 z-[90] flex items-start justify-center p-4 pt-24">
            <motion.button
              type="button"
              className="absolute inset-0 bg-black/70 backdrop-blur-sm"
              aria-label="Close command menu"
              onClick={() => setOpen(false)}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            />

            <motion.div
              className="relative w-full max-w-2xl overflow-hidden rounded-[2rem] border border-[var(--crm-border)] bg-[var(--crm-surface)] text-[var(--crm-text)] shadow-[var(--crm-shadow-soft)]"
              initial={{ opacity: 0, y: 18, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 14, scale: 0.98 }}
              transition={{ duration: 0.18, ease: "easeOut" }}
            >
              <Command label="Tadamun command menu">
                <div className="flex items-center gap-3 border-b border-[var(--crm-border)] px-4">
                  <Search size={18} className="text-[var(--crm-text-muted)]" />
                  <Command.Input
                    autoFocus
                    placeholder="Search pages and actions..."
                    className="h-14 flex-1 bg-transparent text-sm outline-none placeholder:text-[var(--crm-text-muted)]"
                  />
                  <button
                    type="button"
                    onClick={() => setOpen(false)}
                    className="grid h-9 w-9 place-items-center rounded-xl text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-primary)]"
                    aria-label="Close command menu"
                  >
                    <X size={17} />
                  </button>
                </div>

                <Command.List className="max-h-[420px] overflow-y-auto p-3">
                  <Command.Empty className="px-3 py-8 text-center text-sm text-[var(--crm-text-muted)]">
                    No matching command.
                  </Command.Empty>

                  <Command.Group heading="Navigate" className="text-xs font-semibold uppercase tracking-[0.16em] text-[var(--crm-text-muted)]">
                    {commandItems.map((item) => {
                      const Icon = item.icon;

                      return (
                        <Command.Item
                          key={item.path}
                          value={item.label}
                          onSelect={() => runCommand(item.path)}
                          className="mt-1 flex cursor-pointer items-center gap-3 rounded-2xl px-3 py-3 text-sm text-[var(--crm-text)] outline-none aria-selected:bg-violet-500/10"
                        >
                          <span className="grid h-9 w-9 place-items-center rounded-xl bg-violet-500/10 text-[var(--crm-primary)] ring-1 ring-violet-300/20">
                            <Icon size={17} />
                          </span>
                          {item.label}
                        </Command.Item>
                      );
                    })}
                  </Command.Group>
                </Command.List>
              </Command>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </>
  );
}
