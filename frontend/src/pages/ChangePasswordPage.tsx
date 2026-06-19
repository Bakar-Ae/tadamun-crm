import { useState, type FormEvent } from 'react'
import { Eye, EyeOff, LockKeyhole, ShieldCheck } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { GlassCard, PageShell } from '../components/ui'
import { changePassword } from '../services/authService'

type ApiError = {
  response?: {
    data?: {
      message?: string
      fieldErrors?: Record<string, string>
    }
  }
}

export function ChangePasswordPage() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmNewPassword, setConfirmNewPassword] = useState('')
  const [showPasswords, setShowPasswords] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)

    try {
      await changePassword({
        currentPassword,
        newPassword,
        confirmNewPassword,
      })

      const storedUser = localStorage.getItem('user')

      if (storedUser) {
        const user = JSON.parse(storedUser)

        localStorage.setItem(
          'user',
          JSON.stringify({
            ...user,
            passwordChangeRequired: false,
          }),
        )
      }

      setCurrentPassword('')
      setNewPassword('')
      setConfirmNewPassword('')
      setSuccess('Password changed successfully.')

      setTimeout(() => {
        window.location.assign('/dashboard')
      }, 700)
    } catch (error) {
      const apiError = error as ApiError
      const fieldErrors = apiError.response?.data?.fieldErrors
      const message = apiError.response?.data?.message

      if (fieldErrors) {
        setError(Object.values(fieldErrors)[0])
      } else {
        setError(message ?? 'Could not change password')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <AppLayout>
      <PageShell
        title="Account Security"
        description="Update your password to keep your Tadamun account secure."
      >
        <GlassCard className="mx-auto max-w-3xl">
          <div className="mb-6 flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-cyan-400/10 text-[var(--crm-accent-text)] ring-1 ring-cyan-300/20">
              <ShieldCheck size={24} />
            </div>

            <div>
              <p className="text-sm font-semibold text-[var(--crm-accent-text)]">Account Security</p>
              <h2 className="mt-1 text-2xl font-semibold text-[var(--crm-text)]">Password update</h2>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
                {error}
              </div>
            )}

            {success && (
              <div className="rounded-xl border border-emerald-400/30 bg-emerald-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-success-text)]">
                {success}
              </div>
            )}

            <PasswordInput
              label="Current password"
              value={currentPassword}
              show={showPasswords}
              onChange={setCurrentPassword}
              autoComplete="current-password"
            />

            <PasswordInput
              label="New password"
              value={newPassword}
              show={showPasswords}
              onChange={setNewPassword}
              autoComplete="new-password"
            />

            <PasswordInput
              label="Confirm new password"
              value={confirmNewPassword}
              show={showPasswords}
              onChange={setConfirmNewPassword}
              autoComplete="new-password"
            />

            <label className="flex items-center gap-2 text-sm font-medium text-[var(--crm-text-muted)]">
              <input
                type="checkbox"
                checked={showPasswords}
                onChange={(event) => setShowPasswords(event.target.checked)}
                className="h-4 w-4 rounded border-[var(--crm-border)] text-cyan-600"
              />
              Show passwords
            </label>

            <button
              disabled={loading}
              className="inline-flex h-11 items-center justify-center rounded-xl bg-cyan-600 px-5 text-sm font-semibold text-white shadow-sm shadow-cyan-900/20 transition hover:-translate-y-0.5 hover:bg-cyan-700 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? 'Updating...' : 'Update password'}
            </button>
          </form>
        </GlassCard>
      </PageShell>
    </AppLayout>
  )
}

type PasswordInputProps = {
  label: string
  value: string
  show: boolean
  onChange: (value: string) => void
  autoComplete: string
}

function PasswordInput({ label, value, show, onChange, autoComplete }: PasswordInputProps) {
  return (
    <div>
      <label className="mb-2 block text-sm font-semibold text-[var(--crm-text)]">{label}</label>
      <div className="relative">
        <LockKeyhole
          size={18}
          className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--crm-text-muted)]"
        />

        <input
          className="crm-focus h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] pl-10 pr-10 text-sm text-[var(--crm-text)] transition focus:border-cyan-400"
          type={show ? 'text' : 'password'}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          autoComplete={autoComplete}
          required
        />

        <div className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-[var(--crm-text-muted)]">
          {show ? <EyeOff size={18} /> : <Eye size={18} />}
        </div>
      </div>
    </div>
  )
}
