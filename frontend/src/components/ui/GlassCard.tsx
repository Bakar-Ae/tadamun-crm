import type { ReactNode } from 'react'
import { motion } from 'framer-motion'
import { cn } from '../../lib/cn'

type GlassCardProps = {
  children: ReactNode
  className?: string
}

export function GlassCard({ children, className }: GlassCardProps) {
  return (
    <motion.div
      className={cn('crm-glass rounded-2xl p-5 text-[var(--crm-text)]', className)}
      whileHover={{ y: -2 }}
      transition={{ duration: 0.2, ease: 'easeOut' }}
    >
      {children}
    </motion.div>
  )
}