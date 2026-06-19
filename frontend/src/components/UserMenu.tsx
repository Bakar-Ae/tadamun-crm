import { useState } from "react";
import { KeyRound, LogOut, UserRound } from "lucide-react";
import { useNavigate } from "react-router";

type StoredUser = {
  fullName?: string;
  email?: string;
  role?: string;
};

function getStoredUser() {
  const storedUser = localStorage.getItem("user");

  if (!storedUser) {
    return null;
  }

  try {
    return JSON.parse(storedUser) as StoredUser;
  } catch {
    return null;
  }
}

function formatRole(role?: string) {
  if (!role) {
    return null;
  }

  const cleanRole = role.replace("ROLE_", "").replaceAll("_", " ").toLowerCase();

  return `${cleanRole.charAt(0).toUpperCase()}${cleanRole.slice(1)} access`;
}

type UserMenuProps = {
  onLogout: () => void;
};

export function UserMenu({ onLogout }: UserMenuProps) {
  const [open, setOpen] = useState(false);
  const user = getStoredUser();
  const navigate = useNavigate();
  const roleLabel = formatRole(user?.role);

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="grid h-10 w-10 place-items-center rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] text-[var(--crm-text)] shadow-sm transition hover:border-violet-300 hover:text-[var(--crm-primary)]"
        aria-label="Open account menu"
      >
        <UserRound size={18} />
      </button>

      {open && (
        <>
          <button
            type="button"
            className="fixed inset-0 z-40 cursor-default"
            aria-label="Close user menu"
            onClick={() => setOpen(false)}
          />
          <div className="absolute right-0 top-12 z-50 w-72 rounded-3xl border border-[var(--crm-border)] bg-[var(--crm-surface)] p-3 text-[var(--crm-text)] shadow-[var(--crm-shadow-soft)]">
            <div className="rounded-xl bg-[var(--crm-surface-soft)] p-3">
              <p className="text-sm font-semibold">
                My account
              </p>
              <p className="mt-1 truncate text-xs text-[var(--crm-text-muted)]">
                {user?.email ?? user?.fullName ?? "Signed in"}
              </p>
              {roleLabel && (
                <p className="mt-2 inline-flex rounded-full bg-violet-500/10 px-2 py-1 text-[10px] font-bold text-[var(--crm-accent-text)]">
                  {roleLabel}
                </p>
              )}
            </div>

            <button
              type="button"
              onClick={() => {
                setOpen(false);
                navigate("/change-password");
              }}
              className="mt-2 flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-medium text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-text)]"
            >
              <KeyRound size={17} />
              Account security
            </button>

            <button
              type="button"
              onClick={onLogout}
              className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-medium text-red-400 transition hover:bg-red-500/15"
            >
              <LogOut size={17} />
              Logout
            </button>

            <div className="mt-3 flex items-center gap-2 border-t border-[var(--crm-border)] pt-3 text-xs text-[var(--crm-text-muted)]">
              <UserRound size={14} />
              Account options
            </div>
          </div>
        </>
      )}
    </div>
  );
}
