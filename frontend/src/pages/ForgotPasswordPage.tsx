import { useState } from 'react'
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

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
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
    <main className="relative min-h-screen overflow-hidden bg-slate-950 text-white">
      <div className="absolute inset-0 bg-[linear-gradient(to_right,rgba(148,163,184,0.08)_1px,transparent_1px),linear-gradient(to_bottom,rgba(148,163,184,0.08)_1px,transparent_1px)] bg-[size:44px_44px]" />
      <div className="absolute inset-x-0 top-0 h-72 bg-gradient-to-b from-blue-600/25 to-transparent" />

      <section className="relative z-10 flex min-h-screen items-center justify-center px-4 py-10">
        <div className="w-full max-w-md rounded-2xl border border-white/10 bg-white p-6 text-slate-950 shadow-2xl shadow-slate-950/40 sm:p-8">
          <div className="mb-8">
            <div className="mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-950 text-white shadow-lg">
              <ShieldCheck size={24} />
            </div>

            <p className="text-sm font-semibold text-blue-700">Account recovery</p>
            <h1 className="mt-2 text-3xl font-semibold">Forgot password?</h1>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              Enter your email and we will send password reset instructions if the account exists.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="mb-2 block text-sm font-semibold text-slate-700">Email</label>
              <div className="relative">
                <Mail
                  size={18}
                  className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                />
                <input
                  className="h-12 w-full rounded-xl border border-slate-300 bg-white pl-10 pr-3 text-sm outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  type="email"
                  autoComplete="email"
                  required
                />
              </div>
            </div>

            {success && (
              <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
                If an account exists, reset instructions have been sent.
              </div>
            )}

            {error && (
              <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
                {error}
              </div>
            )}

            <button
              className="flex h-12 w-full items-center justify-center rounded-xl bg-blue-600 px-4 text-sm font-semibold text-white shadow-lg shadow-blue-600/25 transition hover:-translate-y-0.5 hover:bg-blue-700 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={loading}
            >
              {loading ? 'Sending...' : 'Send reset link'}
            </button>
          </form>

          <button
            type="button"
            onClick={() => window.location.assign('/')}
            className="mt-6 flex items-center gap-2 text-sm font-semibold text-slate-500 transition hover:text-slate-950"
          >
            <ArrowLeft size={16} />
            Back to login
          </button>
        </div>
      </section>
    </main>
  )
}