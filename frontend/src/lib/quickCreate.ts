import type { QuickCreateKind } from '../components/QuickCreateMenu'

export const quickCreateEventName = 'crm-open-quick-create'

export function openQuickCreate(kind: QuickCreateKind) {
  window.dispatchEvent(
    new CustomEvent<{ kind: QuickCreateKind }>(quickCreateEventName, {
      detail: { kind },
    }),
  )
}
