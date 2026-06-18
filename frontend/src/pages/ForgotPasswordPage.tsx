import { useState, type FormEvent } from 'react'
import { motion } from 'framer-motion'
import { ArrowLeft, Mail, ShieldCheck } from 'lucide-react'
import { forgotPassword } from '../services/authService'

type ApiError = {
  response?: {
    data?: {
      message?: string
    }
  }
}

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSuccess(false)
    setLoading(true)

    try {
      await forgotPassword({ email })
      setSuccess(true)
      setEmail('')
    } catch (error) {
      const apiError = error as ApiError
      setError(apiError.response?.data?.message ?? 'Password reset request failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="relative min-h-screen overflow-hidden bg-[var(--crm-bg)] text-[var(--crm-text)]">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(65,192,242,0.2),transparent_30rem),radial-gradient(circle_at_80%_0%,rgba(2,245,161,0.1),transparent_24rem)]" />
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(to_right,rgba(173,223,241,0.05)_1px,transparent_1px),linear-gradient(to_bottom,rgba(173,223,241,0.05)_1px,transparent_1px)] bg-[size:48px_48px]" />

      <section className="relative z-10 flex min-h-screen items-center justify-center px-4 py-10">
        <motion.div
          className="w-full max-w-md rounded-3xl border border-[var(--crm-border)] bg-[var(--crm-surface-glass)] p-6 shadow-2xl backdrop-blur-xl sm:p-8"
          initial={{ opacity: 0, y: 18, scale: 0.98 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ duration: 0.35, ease: 'easeOut' }}
        >
          <div className="mb-8">
            <div className="mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-cyan-400/10 text-[var(--crm-accent-text)] ring-1 ring-cyan-300/20">
              <ShieldCheck size={24} />
            </div>

            <p className="text-sm font-semibold text-[var(--crm-accent-text)]">Tadamun account recovery</p>
            <h1 className="mt-2 text-3xl font-semibold text-[var(--crm-text)]">Forgot password?</h1>
            <p className="mt-2 text-sm leading-6 text-[var(--crm-text-muted)]">
              Enter your email and we will send reset instructions if the account exists.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="mb-2 block text-sm font-semibold text-[var(--crm-text)]">Email</label>
              <div className="relative">
                <Mail
                  size={18}
                  className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--crm-text-muted)]"
                />
                <input
                  className="crm-focus h-12 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] pl-10 pr-3 text-sm text-[var(--crm-text)] transition focus:border-cyan-400"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  type="email"
                  autoComplete="email"
                  required
                />
              </div>
            </div>

            {success && (
              <div className="rounded-xl border border-emerald-400/30 bg-emerald-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-success-text)]">
                If an account exists, reset instructions have been sent.
              </div>
            )}

            {error && (
              <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
                {error}
              </div>
            )}

            <button
              className="flex h-12 w-full items-center justify-center rounded-xl bg-cyan-600 px-4 text-sm font-semibold text-white shadow-lg shadow-cyan-900/20 transition hover:-translate-y-0.5 hover:bg-cyan-700 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={loading}
            >
              {loading ? 'Sending...' : 'Send reset link'}
            </button>
          </form>

          <button
            type="button"
            onClick={() => window.location.assign('/')}
            className="mt-6 flex items-center gap-2 text-sm font-semibold text-[var(--crm-text-muted)] transition hover:text-[var(--crm-text)]"
          >
            <ArrowLeft size={16} />
            Back to login
          </button>
        </motion.div>
      </section>
    </main>
  )
}
