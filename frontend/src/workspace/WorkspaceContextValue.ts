import { createContext } from 'react'
import type { Workspace } from '../services/workspaceService'

export type WorkspaceStatus = 'loading' | 'ready' | 'error'

export type WorkspaceContextValue = {
  workspaces: Workspace[]
  activeWorkspace: Workspace | null
  status: WorkspaceStatus
  selectWorkspace: (organizationId: number) => void
  reloadWorkspaces: () => Promise<void>
}

export const WorkspaceContext = createContext<WorkspaceContextValue | null>(
  null,
)
