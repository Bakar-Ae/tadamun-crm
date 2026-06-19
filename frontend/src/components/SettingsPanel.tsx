import { useEffect, useState } from "react";
import { SlidersHorizontal } from "lucide-react";
import { Modal } from "./ui";
import {
  getDashboardPreferences,
  saveDashboardPreferences,
  type DashboardPreferences,
} from "../lib/dashboardPreferences";

export function SettingsPanel() {
  const [open, setOpen] = useState(false);
  const [preferences, setPreferences] = useState(getDashboardPreferences);
  const [theme, setTheme] = useState(() =>
    document.documentElement.dataset.theme === "midnight" ? "midnight" : "luxe",
  );

  function updatePreferences(next: DashboardPreferences) {
    setPreferences(next);
    saveDashboardPreferences(next);
  }

  function updateTheme(nextTheme: "luxe" | "midnight") {
    setTheme(nextTheme);
  }

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("crm-theme", theme);
  }, [theme]);

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="grid h-10 w-10 place-items-center rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface-soft)] text-[var(--crm-text-muted)] transition hover:border-cyan-300/40 hover:text-[var(--crm-text)]"
        aria-label="Open workspace settings"
      >
        <SlidersHorizontal size={17} />
      </button>

      <Modal
        open={open}
        title="Workspace settings"
        description="Set the workspace theme and display density."
        onClose={() => setOpen(false)}
      >
        <div className="space-y-6">
          <section>
            <h3 className="text-sm font-semibold text-[var(--crm-text)]">
              Theme
            </h3>
            <div className="mt-3 grid gap-3 sm:grid-cols-2">
              {[
                {
                  id: "luxe",
                  label: "Luxe Light",
                  description: "Bright SaaS workspace",
                },
                {
                  id: "midnight",
                  label: "Midnight Executive",
                  description: "Dark purple command room",
                },
              ].map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => updateTheme(item.id as "luxe" | "midnight")}
                  className={`rounded-3xl border p-4 text-left transition ${
                    theme === item.id
                      ? "border-violet-300 bg-violet-500/10 text-[var(--crm-text)] shadow-[0_14px_32px_rgba(109,93,251,0.14)]"
                      : "border-[var(--crm-border)] bg-[var(--crm-surface-soft)] text-[var(--crm-text-muted)] hover:text-[var(--crm-text)]"
                  }`}
                >
                  <span className="block text-sm font-semibold">{item.label}</span>
                  <span className="mt-1 block text-xs">{item.description}</span>
                  <span className="mt-3 block h-2 rounded-full bg-[var(--crm-brand-gradient)]" />
                </button>
              ))}
            </div>
          </section>

          <section>
            <h3 className="text-sm font-semibold text-[var(--crm-text)]">
              Density
            </h3>
            <div className="mt-3 grid grid-cols-2 gap-3">
              {(["comfortable", "compact"] as const).map((density) => (
                <button
                  key={density}
                  type="button"
                  onClick={() => updatePreferences({ ...preferences, density })}
                  className={`h-11 rounded-xl border px-3 text-sm font-semibold capitalize transition ${
                    preferences.density === density
                      ? "border-violet-300/50 bg-violet-500/10 text-[var(--crm-text)]"
                      : "border-[var(--crm-border)] bg-[var(--crm-surface-soft)] text-[var(--crm-text-muted)] hover:text-[var(--crm-text)]"
                  }`}
                >
                  {density}
                </button>
              ))}
            </div>
          </section>
        </div>
      </Modal>
    </>
  );
}
