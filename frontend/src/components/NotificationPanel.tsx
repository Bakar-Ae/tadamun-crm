import { useEffect, useState } from "react";
import { Bell } from "lucide-react";
import { Link } from "react-router";
import { getUnreadNotificationCount } from "../services/notificationService";

export function NotificationPanel() {
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    let mounted = true;

    getUnreadNotificationCount()
      .then((response) => {
        if (mounted) {
          setUnreadCount(response.unreadCount);
        }
      })
      .catch(() => {
        if (mounted) {
          setUnreadCount(0);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="relative grid h-10 w-10 place-items-center rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] text-[var(--crm-text-muted)] shadow-sm transition hover:border-violet-300 hover:text-[var(--crm-text)]"
        aria-label="Open notifications"
      >
        <Bell size={17} />
        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 grid min-h-5 min-w-5 place-items-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <>
          <button
            type="button"
            className="fixed inset-0 z-40 cursor-default"
            aria-label="Close notifications"
            onClick={() => setOpen(false)}
          />
          <div className="absolute right-0 top-12 z-50 w-80 rounded-3xl border border-[var(--crm-border)] bg-[var(--crm-surface)] p-4 text-[var(--crm-text)] shadow-[var(--crm-shadow-soft)]">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-semibold">Notifications</h3>
                <p className="mt-1 text-xs text-[var(--crm-text-muted)]">
                  {unreadCount} unread
                </p>
              </div>
            </div>

            <Link
              to="/notifications"
              onClick={() => setOpen(false)}
              className="crm-primary-action mt-4 flex h-11 items-center justify-center rounded-2xl text-sm font-semibold transition hover:-translate-y-0.5"
            >
              Open inbox
            </Link>
          </div>
        </>
      )}
    </div>
  );
}
