import { useState } from 'react'
import { Eye, EyeOff, LockKeyhole, ShieldCheck } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
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

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
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

      setCurrentPassword('')
      setNewPassword('')
      setConfirmNewPassword('')
      setSuccess('Password changed successfully. Please use the new password next time you log in.')
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
      <div className="mx-auto max-w-3xl space-y-6">
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="bg-slate-950 px-6 py-7 text-white">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-600">
                <ShieldCheck size={24} />
              </div>

              <div>
                <p className="text-sm font-medium text-blue-300">Account Security</p>
                <h2 className="mt-1 text-2xl font-semibold">Change Password</h2>
                <p className="mt-1 text-sm text-slate-300">
                  Update your password to keep your CRM account secure.
                </p>
              </div>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5 p-6">
            {error && (
              <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
                {error}
              </div>
            )}

            {success && (
              <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
                {success}
              </div>
            )}

            <PasswordInput
              label="Current Password"
              value={currentPassword}
              show={showPasswords}
              onChange={setCurrentPassword}
            />

            <PasswordInput
              label="New Password"
              value={newPassword}
              show={showPasswords}
              onChange={setNewPassword}
            />

            <PasswordInput
              label="Confirm New Password"
              value={confirmNewPassword}
              show={showPasswords}
              onChange={setConfirmNewPassword}
            />

            <label className="flex items-center gap-2 text-sm font-medium text-slate-600">
              <input
                type="checkbox"
                checked={showPasswords}
                onChange={(event) => setShowPasswords(event.target.checked)}
                className="h-4 w-4 rounded border-slate-300 text-blue-600"
              />
              Show passwords
            </label>

            <button
              disabled={loading}
              className="inline-flex h-11 items-center justify-center rounded-lg bg-blue-600 px-5 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? 'Changing...' : 'Change Password'}
            </button>
          </form>
        </section>
      </div>
    </AppLayout>
  )
}

type PasswordInputProps = {
  label: string
  value: string
  show: boolean
  onChange: (value: string) => void
}

function PasswordInput({ label, value, show, onChange }: PasswordInputProps) {
  return (
    <div>
      <label className="mb-2 block text-sm font-semibold text-slate-700">{label}</label>
      <div className="relative">
        <LockKeyhole
          size={18}
          className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
        />

        <input
          className="h-11 w-full rounded-lg border border-slate-300 bg-white pl-10 pr-10 text-sm outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
          type={show ? 'text' : 'password'}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          autoComplete="new-password"
        />

        <div className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">
          {show ? <EyeOff size={18} /> : <Eye size={18} />}
        </div>
      </div>
    </div>
  )
}