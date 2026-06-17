import { useState } from 'react'
import {
  Activity,
  ArrowRight,
  BarChart3,
  Database,
  Eye,
  EyeOff,
  LockKeyhole,
  Mail,
  ShieldCheck,
  UsersRound,
} from 'lucide-react'
import { login } from '../services/authService'

type ApiError = {
  response?: {
    status?: number
    data?: {
      message?: string
    }
  }
}

export function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      const response = await login({ email, password })

      if (!response.accessToken || !response.refreshToken) {
        setError('Login failed. Please try again.')
        return
      }

      localStorage.setItem('token', response.accessToken)
      localStorage.setItem('refreshToken', response.refreshToken)
      localStorage.setItem(
        'user',
        JSON.stringify({
          userId: response.userId,
          fullName: response.fullName,
          email: response.email,
          role: response.role,
        }),
      )
      window.location.href = '/dashboard'
    } catch (error) {
      const apiError = error as ApiError
      const status = apiError.response?.status
      const message = apiError.response?.data?.message

      if (status === 429) {
        setError(message ?? 'Too many login attempts. Please try again later.')
      } else {
        setError('Invalid email or password')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="relative min-h-screen overflow-hidden bg-slate-950 text-white">
      <div className="absolute inset-0 bg-[linear-gradient(to_right,rgba(148,163,184,0.08)_1px,transparent_1px),linear-gradient(to_bottom,rgba(148,163,184,0.08)_1px,transparent_1px)] bg-[size:44px_44px]" />
      <div className="absolute inset-x-0 top-0 h-72 bg-gradient-to-b from-blue-600/25 to-transparent" />

      <section className="relative z-10 grid min-h-screen lg:grid-cols-[1.1fr_0.9fr]">
        <div className="hidden flex-col justify-between p-10 lg:flex xl:p-14">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-600 font-bold shadow-lg shadow-blue-950/40">
              CRM
            </div>
            <div>
              <h1 className="text-lg font-semibold">Enterprise CRM</h1>
              <p className="text-sm text-slate-400">Sales operations workspace</p>
            </div>
          </div>

          <div className="max-w-2xl">
            <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/10 px-3 py-1.5 text-sm text-blue-100">
              <ShieldCheck size={16} />
              Secure role-based access
            </div>

            <h2 className="text-5xl font-semibold leading-tight tracking-normal xl:text-6xl">
              Manage customers, leads, and work with confidence.
            </h2>

            <p className="mt-5 max-w-xl text-base leading-7 text-slate-300">
              A focused CRM workspace for teams that need clean customer records, lead tracking,
              task ownership, reports, and audit visibility.
            </p>

            <div className="mt-8 grid max-w-2xl grid-cols-3 gap-3">
              <div className="rounded-xl border border-white/10 bg-white/10 p-4 shadow-xl shadow-slate-950/20 backdrop-blur">
                <UsersRound className="text-blue-300" size={24} />
                <p className="mt-4 text-2xl font-semibold">360</p>
                <p className="mt-1 text-xs font-medium uppercase text-slate-400">Customer View</p>
              </div>

              <div className="rounded-xl border border-white/10 bg-white/10 p-4 shadow-xl shadow-slate-950/20 backdrop-blur">
                <Activity className="text-emerald-300" size={24} />
                <p className="mt-4 text-2xl font-semibold">Live</p>
                <p className="mt-1 text-xs font-medium uppercase text-slate-400">Activity</p>
              </div>

              <div className="rounded-xl border border-white/10 bg-white/10 p-4 shadow-xl shadow-slate-950/20 backdrop-blur">
                <BarChart3 className="text-amber-300" size={24} />
                <p className="mt-4 text-2xl font-semibold">Reports</p>
                <p className="mt-1 text-xs font-medium uppercase text-slate-400">Insights</p>
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-white/10 bg-white/10 p-5 shadow-2xl shadow-slate-950/30 backdrop-blur">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold">System Status</p>
                <p className="mt-1 text-sm text-slate-400">Backend, database, and API are ready</p>
              </div>
              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-500/15 text-emerald-300">
                <Database size={22} />
              </div>
            </div>

            <div className="mt-5 h-2 overflow-hidden rounded-full bg-white/10">
              <div className="h-full w-3/4 animate-pulse rounded-full bg-gradient-to-r from-blue-500 via-emerald-400 to-amber-300" />
            </div>
          </div>
        </div>

        <div className="flex min-h-screen items-center justify-center px-4 py-10 sm:px-6 lg:px-10">
          <section className="w-full max-w-md rounded-2xl border border-white/10 bg-white p-6 text-slate-950 shadow-2xl shadow-slate-950/40 sm:p-8">
            <div className="mb-8">
              <div className="mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-950 text-white shadow-lg">
                <LockKeyhole size={24} />
              </div>

              <p className="text-sm font-semibold text-blue-700">Welcome back</p>
              <h2 className="mt-2 text-3xl font-semibold">Sign in to CRM</h2>
              <p className="mt-2 text-sm text-slate-500">
                Enter your account details to continue to the workspace.
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
                  />
                </div>
              </div>

              <div>
                <label className="mb-2 block text-sm font-semibold text-slate-700">Password</label>
                <div className="relative">
                  <LockKeyhole
                    size={18}
                    className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                  />
                  <input
                    className="h-12 w-full rounded-xl border border-slate-300 bg-white pl-10 pr-11 text-sm outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((value) => !value)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 rounded-md p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              {error && (
                <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
                  {error}
                </div>
              )}

              <button
                className="group flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-semibold text-white shadow-lg shadow-blue-600/25 transition hover:-translate-y-0.5 hover:bg-blue-700 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={loading}
              >
                {loading ? 'Signing in...' : 'Sign in'}
                <ArrowRight size={18} className="transition group-hover:translate-x-1" />
              </button>
            </form>

            
          </section>
        </div>
      </section>
    </main>
  )
}
