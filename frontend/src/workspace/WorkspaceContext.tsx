import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  clearStoredWorkspaceId,
  getStoredWorkspaceId,
  setStoredWorkspaceId,
} from '../lib/workspaceStorage'
import {
  getMyWorkspaces,
  type Workspace,
} from '../services/workspaceService'
import {
  WorkspaceContext,
  type WorkspaceContextValue,
  type WorkspaceStatus,
} from './WorkspaceContextValue'

function resolveActiveWorkspace(workspaces: Workspace[]) {
  const storedWorkspaceId = getStoredWorkspaceId()
  const storedWorkspace = workspaces.find(
    (workspace) => workspace.organizationId === storedWorkspaceId,
  )

  if (storedWorkspace) {
    return storedWorkspace
  }

  clearStoredWorkspaceId()

  if (workspaces.length === 1) {
    setStoredWorkspaceId(workspaces[0].organizationId)
    return workspaces[0]
  }

  return null
}

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const hasSession = Boolean(localStorage.getItem('token'))
  const [workspaces, setWorkspaces] = useState<Workspace[]>([])
  const [activeWorkspace, setActiveWorkspace] =
    useState<Workspace | null>(null)
  const [status, setStatus] = useState<WorkspaceStatus>(
    hasSession ? 'loading' : 'ready',
  )

  const applyWorkspaces = useCallback((nextWorkspaces: Workspace[]) => {
    setWorkspaces(nextWorkspaces)
    setActiveWorkspace(resolveActiveWorkspace(nextWorkspaces))
    setStatus('ready')
  }, [])

  useEffect(() => {
    if (!hasSession) return

    let active = true

    getMyWorkspaces()
      .then((nextWorkspaces) => {
        if (active) applyWorkspaces(nextWorkspaces)
      })
      .catch(() => {
        if (active) setStatus('error')
      })

    return () => {
      active = false
    }
  }, [applyWorkspaces, hasSession])

  const reloadWorkspaces = useCallback(async () => {
    if (!localStorage.getItem('token')) {
      clearStoredWorkspaceId()
      setWorkspaces([])
      setActiveWorkspace(null)
      setStatus('ready')
      return
    }

    setStatus('loading')

    try {
      applyWorkspaces(await getMyWorkspaces())
    } catch {
      setStatus('error')
    }
  }, [applyWorkspaces])

  const selectWorkspace = useCallback(
    (organizationId: number) => {
      const workspace = workspaces.find(
        (candidate) => candidate.organizationId === organizationId,
      )

      if (!workspace) return

      setStoredWorkspaceId(workspace.organizationId)
      setActiveWorkspace(workspace)
      window.location.assign('/dashboard')
    },
    [workspaces],
  )

  const value = useMemo<WorkspaceContextValue>(
    () => ({
      workspaces,
      activeWorkspace,
      status,
      selectWorkspace,
      reloadWorkspaces,
    }),
    [
      activeWorkspace,
      reloadWorkspaces,
      selectWorkspace,
      status,
      workspaces,
    ],
  )

  return (
    <WorkspaceContext.Provider value={value}>
      {children}
    </WorkspaceContext.Provider>
  )
}
