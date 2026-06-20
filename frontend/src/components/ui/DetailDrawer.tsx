import { useEffect, type ReactNode } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { X } from 'lucide-react'

type DetailDrawerProps = {
  open: boolean
  title: string
  description?: string
  children: ReactNode
  footer?: ReactNode
  onClose: () => void
  size?: 'md' | 'lg'
}

export function DetailDrawer({
  open,
  title,
  description,
  children,
  footer,
  onClose,
  size = 'lg',
}: DetailDrawerProps) {
  const widthClass = size === 'lg' ? 'max-w-2xl' : 'max-w-xl'

  useEffect(() => {
    if (!open) {
      return
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    document.body.style.overflow = 'hidden'

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = ''
    }
  }, [open, onClose])

  return (
    <AnimatePresence>
      {open ? (
        <div className="fixed inset-0 z-50">
          <motion.button
            type="button"
            aria-label="Close details"
            className="absolute inset-0 bg-slate-950/40 backdrop-blur-sm"
            onClick={onClose}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          />
          <motion.aside
            role="dialog"
            aria-modal="true"
            aria-labelledby="detail-drawer-title"
            className={`absolute right-0 top-0 flex h-full w-full ${widthClass} flex-col border-l border-[var(--crm-border)] bg-[var(--crm-surface)] shadow-2xl`}
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', damping: 28, stiffness: 260 }}
          >
            <header className="flex items-start justify-between gap-4 border-b border-[var(--crm-border)] px-6 py-5">
              <div>
                <h2 id="detail-drawer-title" className="text-xl font-semibold text-[var(--crm-text)]">
                  {title}
                </h2>

                {description ? (
                  <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
                    {description}
                  </p>
                ) : null}
              </div>

              <button
                type="button"
                aria-label="Close details"
                onClick={onClose}
                className="crm-focus inline-flex h-10 w-10 items-center justify-center rounded-xl border border-[var(--crm-border)] text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-text)]"
              >
                <X size={18} />
              </button>
            </header>

            <div className="flex-1 overflow-y-auto px-6 py-5">
              {children}
            </div>

            {footer ? (
              <footer className="border-t border-[var(--crm-border)] px-6 py-4">
                {footer}
              </footer>
            ) : null}
          </motion.aside>
        </div>
      ) : null}
    </AnimatePresence>
  )
}
