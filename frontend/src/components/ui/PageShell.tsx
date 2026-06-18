import type { ReactNode } from 'react'
import { motion } from 'framer-motion'
import { cn } from '../../lib/cn'

type PageShellProps = {
  title: string
  description?: string
  action?: ReactNode
  children: ReactNode
  className?: string
}

export function PageShell({ title, description, action, children, className }: PageShellProps) {
  return (
    <motion.div
      className={cn('space-y-6', className)}
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: 'easeOut' }}
    >
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-cyan-300">
            Enterprise CRM
          </p>
          <h1 className="mt-2 text-3xl font-semibold tracking-normal text-white">{title}</h1>
          {description && <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-400">{description}</p>}
        </div>

        {action && <div className="shrink-0">{action}</div>}
      </div>

      {children}
    </motion.div>
  )
}