import { useEffect, useState } from "react";
import { Bell, LoaderCircle } from "lucide-react";
import toast from "react-hot-toast";
import {
  getNotificationPreferences,
  updateNotificationPreferences,
  type NotificationPreferences,
} from "../services/notificationService";
import {
  getLoadErrorMessage,
  getSaveErrorMessage,
} from "../lib/errors";

type PreferenceKey = keyof NotificationPreferences;

type ToggleRowProps = {
  label: string;
  description: string;
  enabled: boolean;
  onChange: () => void;
};

function ToggleRow({
  label,
  description,
  enabled,
  onChange,
}: ToggleRowProps) {
  return (
    <div className="flex items-center justify-between gap-4 py-3">
      <div>
        <p className="text-sm font-semibold text-[var(--crm-text)]">{label}</p>
        <p className="mt-1 text-xs text-[var(--crm-text-muted)]">
          {description}
        </p>
      </div>

      <button
        type="button"
        role="switch"
        aria-label={`${label}: ${enabled ? "on" : "off"}`}
        aria-checked={enabled ? "true" : "false"}
        title={`${label}: ${enabled ? "on" : "off"}`}
        onClick={onChange}
        className={`relative h-6 w-11 shrink-0 rounded-full transition ${
          enabled ? "bg-violet-600" : "bg-slate-300"
        }`}
      >
        <span
          className={`absolute top-1 h-4 w-4 rounded-full bg-white transition ${
            enabled ? "left-6" : "left-1"
          }`}
        />
      </button>
    </div>
  );
}

export function NotificationPreferencesSection() {
  const [preferences, setPreferences] =
    useState<NotificationPreferences | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;

    getNotificationPreferences()
      .then((response) => {
        if (active) setPreferences(response);
      })
      .catch(() => {
        if (active) {
          setError(getLoadErrorMessage("notification preferences"));
        }
      });

    return () => {
      active = false;
    };
  }, []);

  function toggle(key: PreferenceKey) {
    setPreferences((current) =>
      current
        ? {
            ...current,
            [key]: !current[key],
          }
        : current,
    );
  }

  async function savePreferences() {
    if (!preferences) return;

    setSaving(true);
    setError(null);

    try {
      const saved = await updateNotificationPreferences(preferences);
      setPreferences(saved);
      toast.success("Notification preferences saved");
    } catch {
      setError(getSaveErrorMessage("notification preferences"));
    } finally {
      setSaving(false);
    }
  }

  const rows: Array<{
    key: PreferenceKey;
    label: string;
    description: string;
  }> = [
    {
      key: "emailEnabled",
      label: "Email notifications",
      description: "Receive optional CRM updates by email.",
    },
    {
      key: "inAppEnabled",
      label: "In-app notifications",
      description: "Show optional updates inside Tadamun.",
    },
    {
      key: "taskNotificationsEnabled",
      label: "Task assignments",
      description: "Notify me when work is assigned.",
    },
    {
      key: "customerNotificationsEnabled",
      label: "Customer updates",
      description: "Notify me about customer record changes.",
    },
    {
      key: "leadNotificationsEnabled",
      label: "Lead updates",
      description: "Notify me about lead activity.",
    },
    {
      key: "reportNotificationsEnabled",
      label: "Reports",
      description: "Notify me when a report is ready.",
    },
  ];

  return (
    <section className="border-t border-[var(--crm-border)] pt-6">
      <div className="flex items-center gap-2">
        <Bell size={17} className="text-violet-500" />
        <h3 className="text-sm font-semibold text-[var(--crm-text)]">
          Notifications
        </h3>
      </div>

      {!preferences && !error && (
        <div className="mt-4 flex items-center gap-2 text-sm text-[var(--crm-text-muted)]">
          <LoaderCircle size={16} className="animate-spin" />
          Loading preferences...
        </div>
      )}

      {error && (
        <p className="mt-4 text-sm text-red-600">{error}</p>
      )}

      {preferences && (
        <>
          <div className="mt-3 divide-y divide-[var(--crm-border)]">
            {rows.map((row) => (
              <ToggleRow
                key={row.key}
                label={row.label}
                description={row.description}
                enabled={preferences[row.key]}
                onChange={() => toggle(row.key)}
              />
            ))}
          </div>

          <button
            type="button"
            disabled={saving}
            onClick={savePreferences}
            className="mt-4 h-10 rounded-xl bg-violet-600 px-4 text-sm font-semibold text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {saving ? "Saving..." : "Save preferences"}
          </button>
        </>
      )}
    </section>
  );
}