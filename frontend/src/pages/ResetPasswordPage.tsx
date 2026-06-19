import { useState, type FormEvent } from 'react'
import { motion } from 'framer-motion'
import { ArrowLeft, Eye, EyeOff, LockKeyhole, ShieldCheck } from 'lucide-react'
import { resetPassword } from '../services/authService'

type ApiError = {
  response?: {
    data?: {
      message?: string
    }
  }
}

export function ResetPasswordPage() {
  const params = new URLSearchParams(window.location.search)
  const token = params.get('token') ?? ''

  const [newPassword, setNewPassword] = useState('')
  const [confirmNewPassword, setConfirmNewPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSuccess(false)
    setLoading(true)

    try {
      await resetPassword({
        token,
        newPassword,
        confirmNewPassword,
      })

      setSuccess(true)
      setNewPassword('')
      setConfirmNewPassword('')
    } catch (error) {
      const apiError = error as ApiError
      setError(apiError.response?.data?.message ?? 'Password reset failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="relative min-h-screen overflow-hidden bg-[var(--crm-bg)] text-[var(--crm-text)]">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(109,93,251,0.18),transparent_30rem),radial-gradient(circle_at_80%_0%,rgba(56,189,248,0.12),transparent_24rem)]" />
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(to_right,rgba(109,93,251,0.05)_1px,transparent_1px),linear-gradient(to_bottom,rgba(109,93,251,0.05)_1px,transparent_1px)] bg-[size:48px_48px]" />

      <section className="relative z-10 flex min-h-screen items-center justify-center px-4 py-10">
        <motion.div
          className="w-full max-w-md rounded-3xl border border-[var(--crm-border)] bg-[var(--crm-surface-glass)] p-6 shadow-2xl backdrop-blur-xl sm:p-8"
          initial={{ opacity: 0, y: 18, scale: 0.98 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ duration: 0.35, ease: 'easeOut' }}
        >
          <div className="mb-8">
            <div className="mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)] ring-1 ring-violet-300/25">
              <ShieldCheck size={24} />
            </div>

            <p className="text-sm font-semibold text-[var(--crm-accent-text)]">Secure reset</p>
            <h1 className="mt-2 text-3xl font-semibold text-[var(--crm-text)]">Create new password</h1>
            <p className="mt-2 text-sm leading-6 text-[var(--crm-text-muted)]">
              Choose a strong password with uppercase, lowercase, and a number.
            </p>
          </div>

          {!token && (
            <div className="mb-4 rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
              Reset token is missing. Please request a new password reset link.
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <PasswordField
              label="New password"
              value={newPassword}
              show={showPassword}
              onChange={setNewPassword}
              withToggle
              onToggle={() => setShowPassword((value) => !value)}
            />

            <PasswordField
              label="Confirm password"
              value={confirmNewPassword}
              show={showPassword}
              onChange={setConfirmNewPassword}
            />

            {success && (
              <div className="rounded-xl border border-emerald-400/30 bg-emerald-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-success-text)]">
                Password reset successful. You can now sign in.
              </div>
            )}

            {error && (
              <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
                {error}
              </div>
            )}

            <button
              className="flex h-12 w-full items-center justify-center rounded-xl bg-[var(--crm-brand-gradient)] px-4 text-sm font-semibold text-white shadow-[0_14px_34px_rgba(109,93,251,0.22)] transition hover:-translate-y-0.5 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={loading || !token}
            >
              {loading ? 'Resetting...' : 'Reset password'}
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

type PasswordFieldProps = {
  label: string
  value: string
  show: boolean
  onChange: (value: string) => void
  withToggle?: boolean
  onToggle?: () => void
}

function PasswordField({ label, value, show, onChange, withToggle, onToggle }: PasswordFieldProps) {
  return (
    <div>
      <label className="mb-2 block text-sm font-semibold text-[var(--crm-text)]">{label}</label>
      <div className="relative">
        <LockKeyhole
          size={18}
          className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--crm-text-muted)]"
        />
        <input
          className="crm-focus h-12 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] pl-10 pr-11 text-sm text-[var(--crm-text)] transition focus:border-[var(--crm-primary)]"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          type={show ? 'text' : 'password'}
          autoComplete="new-password"
          required
        />
        {withToggle && (
          <button
            type="button"
            onClick={onToggle}
            className="absolute right-3 top-1/2 -translate-y-1/2 rounded-lg p-1 text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-text)]"
            aria-label={show ? 'Hide password' : 'Show password'}
          >
            {show ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        )}
      </div>
    </div>
  )
}
