type PaginationBarProps = {
  page: number
  totalPages: number
  totalElements: number
  pageSize: number
  onPrevious: () => void
  onNext: () => void
  disabled?: boolean
}

export function PaginationBar({
  page,
  totalPages,
  totalElements,
  pageSize,
  onPrevious,
  onNext,
  disabled = false,
}: PaginationBarProps) {
  const currentPage = page + 1
  const hasPrevious = page > 0
  const hasNext = currentPage < totalPages

  return (
    <div className="flex flex-col gap-3 border-t border-[var(--crm-border)] px-5 py-4 text-sm text-[var(--crm-text-muted)] sm:flex-row sm:items-center sm:justify-between">
      <span>
        Page {totalPages === 0 ? 0 : currentPage} of {totalPages} · {totalElements} total · {pageSize} per page
      </span>

      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={onPrevious}
          disabled={disabled || !hasPrevious}
          className="h-9 rounded-xl border border-[var(--crm-border)] px-3 font-semibold text-[var(--crm-text)] transition hover:bg-violet-500/10 disabled:cursor-not-allowed disabled:opacity-50"
        >
          Previous
        </button>

        <button
          type="button"
          onClick={onNext}
          disabled={disabled || !hasNext}
          className="h-9 rounded-xl border border-[var(--crm-border)] px-3 font-semibold text-[var(--crm-text)] transition hover:bg-violet-500/10 disabled:cursor-not-allowed disabled:opacity-50"
        >
          Next
        </button>
      </div>
    </div>
  )
}