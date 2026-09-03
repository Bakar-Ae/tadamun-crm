export const activeWorkspaceStorageKey = 'crm-active-workspace-id'

export function getStoredWorkspaceId(): number | null {
  const rawValue = localStorage.getItem(activeWorkspaceStorageKey)

  if (!rawValue) return null

  const workspaceId = Number(rawValue)

  if (!Number.isInteger(workspaceId) || workspaceId <= 0) {
    clearStoredWorkspaceId()
    return null
  }

  return workspaceId
}

export function setStoredWorkspaceId(workspaceId: number) {
  localStorage.setItem(activeWorkspaceStorageKey, String(workspaceId))
}

export function clearStoredWorkspaceId() {
  localStorage.removeItem(activeWorkspaceStorageKey)
}
