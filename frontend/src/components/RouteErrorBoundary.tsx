import { Component, type ReactNode } from 'react'

type RouteErrorBoundaryProps = {
  children: ReactNode
}

type RouteErrorBoundaryState = {
  hasError: boolean
}

const RETRY_TIMESTAMP_KEY = 'crm-route-retry-at'
const RETRY_COOLDOWN_MS = 15_000

function isChunkLoadError(error: unknown) {
  const message = error instanceof Error ? error.message : String(error)

  return (
    message.includes('Failed to fetch dynamically imported module') ||
    message.includes('Loading chunk') ||
    message.includes('Importing a module script failed')
  )
}

export class RouteErrorBoundary extends Component<
  RouteErrorBoundaryProps,
  RouteErrorBoundaryState
> {
  state: RouteErrorBoundaryState = {
    hasError: false,
  }

  static getDerivedStateFromError(): RouteErrorBoundaryState {
    return { hasError: true }
  }

  componentDidCatch(error: unknown) {
    if (!isChunkLoadError(error)) {
      return
    }

    const lastRetryAt = Number(sessionStorage.getItem(RETRY_TIMESTAMP_KEY) ?? 0)

    if (Date.now() - lastRetryAt > RETRY_COOLDOWN_MS) {
      sessionStorage.setItem(RETRY_TIMESTAMP_KEY, String(Date.now()))
      window.location.reload()
    }
  }

  private retry = () => {
    sessionStorage.removeItem(RETRY_TIMESTAMP_KEY)
    window.location.reload()
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children
    }

    return (
      <main className="grid min-h-screen place-items-center bg-[var(--crm-bg)] px-6 text-[var(--crm-text)]">
        <section className="w-full max-w-md rounded-3xl border border-[var(--crm-border)] bg-[var(--crm-surface)] p-7 text-center shadow-[var(--crm-shadow-soft)]">
          <h1 className="text-xl font-semibold">Tadamun could not finish loading</h1>
          <p className="mt-2 text-sm leading-6 text-[var(--crm-text-muted)]">
            The connection was interrupted. Retry to continue.
          </p>
          <button
            type="button"
            onClick={this.retry}
            className="mt-6 h-11 rounded-xl bg-[var(--crm-primary)] px-5 text-sm font-semibold text-white transition hover:opacity-90"
          >
            Retry
          </button>
        </section>
      </main>
    )
  }
}
