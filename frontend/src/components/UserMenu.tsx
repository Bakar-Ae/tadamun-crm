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

type UserMenuProps = {
  onLogout: () => void;
};

export function UserMenu({ onLogout }: UserMenuProps) {
  const [open, setOpen] = useState(false);
  const user = getStoredUser();
  const navigate = useNavigate();
  const initials =
    user?.fullName
      ?.split(" ")
      .slice(0, 2)
      .map((part) => part[0])
      .join("")
      .toUpperCase() || "TD";

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="flex h-10 items-center gap-2 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] pl-2 pr-3 text-sm font-semibold text-[var(--crm-text)] shadow-sm transition hover:border-violet-300"
      >
        <span className="grid h-7 w-7 place-items-center rounded-xl bg-violet-500/12 text-xs text-[var(--crm-primary)]">
          {initials}
        </span>
        <span className="hidden max-w-28 truncate sm:inline">
          {user?.fullName ?? "Account"}
        </span>
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
                {user?.fullName ?? "Tadamun user"}
              </p>
              <p className="mt-1 truncate text-xs text-[var(--crm-text-muted)]">
                {user?.email ?? "Signed in"}
              </p>
              {user?.role && (
                <p className="mt-2 inline-flex rounded-full bg-violet-500/10 px-2 py-1 text-[10px] font-bold text-[var(--crm-accent-text)]">
                  {user.role.replace("ROLE_", "")}
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
              Change password
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
              Secure local workspace
            </div>
          </div>
        </>
      )}
    </div>
  );
}
