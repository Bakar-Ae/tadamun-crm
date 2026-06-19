type LoadingStateProps = {
  message?: string
}

export function LoadingState({ message = 'Loading...' }: LoadingStateProps) {
  return (
    <div className="flex items-center justify-center gap-3 px-5 py-8 text-sm font-medium text-[var(--crm-text-muted)]">
      <span className="h-2.5 w-2.5 animate-pulse rounded-full bg-[var(--crm-primary)]" />
      <span>{message}</span>
    </div>
  )
}