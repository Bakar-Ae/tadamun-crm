import type { ReactNode } from 'react'
import { motion } from 'framer-motion'
import { cn } from '../../lib/cn'

type GlassCardProps = {
  children: ReactNode
  className?: string
  interactive?: boolean
}

export function GlassCard({ children, className, interactive = false }: GlassCardProps) {
  return (
    <motion.div
      className={cn('crm-glass rounded-3xl p-5 text-[var(--crm-text)]', className)}
      whileHover={interactive ? { y: -2 } : undefined}
      transition={{ duration: 0.2, ease: 'easeOut' }}
    >
      {children}
    </motion.div>
  )
}
