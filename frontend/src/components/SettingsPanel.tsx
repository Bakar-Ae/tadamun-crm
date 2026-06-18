import { useState } from "react";
import { SlidersHorizontal } from "lucide-react";
import { Modal } from "./ui";
import {
  getDashboardPreferences,
  saveDashboardPreferences,
  type DashboardPreferences,
  type DashboardWidgetKey,
} from "../lib/dashboardPreferences";

const widgetLabels: Array<{ key: DashboardWidgetKey; label: string }> = [
  { key: "kpis", label: "KPI grid" },
  { key: "pipeline", label: "Task status" },
  { key: "tasks", label: "Account status" },
  { key: "distribution", label: "CRM totals" },
  { key: "focus", label: "Review list" },
];

export function SettingsPanel() {
  const [open, setOpen] = useState(false);
  const [preferences, setPreferences] = useState(getDashboardPreferences);

  function updatePreferences(next: DashboardPreferences) {
    setPreferences(next);
    saveDashboardPreferences(next);
  }

  function updateWidget(key: DashboardWidgetKey, checked: boolean) {
    updatePreferences({
      ...preferences,
      widgets: {
        ...preferences.widgets,
        [key]: checked,
      },
    });
  }

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
        description="Customize dashboard density and visible widgets."
        onClose={() => setOpen(false)}
      >
        <div className="space-y-6">
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
                      ? "border-cyan-300/50 bg-cyan-400/15 text-[var(--crm-text)]"
                      : "border-[var(--crm-border)] bg-[var(--crm-surface-soft)] text-[var(--crm-text-muted)] hover:text-[var(--crm-text)]"
                  }`}
                >
                  {density}
                </button>
              ))}
            </div>
          </section>

          <section>
            <h3 className="text-sm font-semibold text-[var(--crm-text)]">
              Dashboard widgets
            </h3>
            <div className="mt-3 space-y-2">
              {widgetLabels.map((widget) => (
                <label
                  key={widget.key}
                  className="flex cursor-pointer items-center justify-between rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface-soft)] px-3 py-3 text-sm font-medium text-[var(--crm-text)]"
                >
                  {widget.label}
                  <input
                    type="checkbox"
                    checked={preferences.widgets[widget.key]}
                    onChange={(event) =>
                      updateWidget(widget.key, event.target.checked)
                    }
                    className="h-4 w-4 accent-cyan-400"
                  />
                </label>
              ))}
            </div>
          </section>
        </div>
      </Modal>
    </>
  );
}
