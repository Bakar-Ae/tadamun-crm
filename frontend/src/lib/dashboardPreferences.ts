export type DashboardWidgetKey =
  | "kpis"
  | "pipeline"
  | "tasks"
  | "distribution"
  | "focus";

export type DashboardPreferences = {
  density: "comfortable" | "compact";
  widgets: Record<DashboardWidgetKey, boolean>;
};

const defaultDashboardPreferences: DashboardPreferences = {
  density: "comfortable",
  widgets: {
    kpis: true,
    pipeline: true,
    tasks: true,
    distribution: true,
    focus: true,
  },
};

export function getDashboardPreferences(): DashboardPreferences {
  const stored = localStorage.getItem("crm-dashboard-preferences");

  if (!stored) {
    return defaultDashboardPreferences;
  }

  try {
    const parsed = JSON.parse(stored) as Partial<DashboardPreferences>;

    return {
      ...defaultDashboardPreferences,
      ...parsed,
      widgets: {
        ...defaultDashboardPreferences.widgets,
        ...(parsed.widgets ?? {}),
      },
    };
  } catch {
    localStorage.removeItem("crm-dashboard-preferences");
    return defaultDashboardPreferences;
  }
}

export function saveDashboardPreferences(preferences: DashboardPreferences) {
  localStorage.setItem("crm-dashboard-preferences", JSON.stringify(preferences));
  document.documentElement.dataset.density = preferences.density;
  window.dispatchEvent(new Event("crm-dashboard-settings-changed"));
}
