import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "../../lib/cn";

type ModalProps = {
  open: boolean;
  title: string;
  description?: string;
  children: ReactNode;
  onClose: () => void;
  className?: string;
};

export function Modal({
  open,
  title,
  description,
  children,
  onClose,
  className,
}: ModalProps) {
  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-[80] flex items-end justify-center p-3 sm:items-center sm:p-6">
          <motion.button
            type="button"
            className="absolute inset-0 bg-black/70 backdrop-blur-sm"
            aria-label="Close modal"
            onClick={onClose}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          />

          <motion.section
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-title"
            className={cn(
              "relative max-h-[90dvh] w-full overflow-y-auto rounded-[2rem] border border-[var(--crm-border)] bg-[var(--crm-surface)] p-5 text-[var(--crm-text)] shadow-[var(--crm-shadow-soft)] sm:max-w-xl",
              className,
            )}
            initial={{ opacity: 0, y: 24, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 18, scale: 0.98 }}
            transition={{ duration: 0.2, ease: "easeOut" }}
          >
            <div className="mb-5 flex items-start justify-between gap-4">
              <div>
                <h2 id="modal-title" className="text-xl font-semibold">
                  {title}
                </h2>
                {description && (
                  <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
                    {description}
                  </p>
                )}
              </div>

              <button
                type="button"
                onClick={onClose}
                className="grid h-10 w-10 shrink-0 place-items-center rounded-xl text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-primary)]"
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            {children}
          </motion.section>
        </div>
      )}
    </AnimatePresence>
  );
}
