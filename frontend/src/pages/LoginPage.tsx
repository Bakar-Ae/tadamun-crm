import { useState } from 'react'
import { motion } from 'framer-motion'
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
  Sparkles,
  UsersRound,
} from 'lucide-react'
import { login } from '../services/authService'
import { cn } from '../lib/cn'
import bgCommandCenter from '../assets/login-bg-command-center.webp'
import tadamunLogo from '../assets/tadamun-logo.svg'
import { PreLoginIntro } from '../components/PreLoginIntro'

type ApiError = {
  response?: {
    status?: number
    data?: {
      message?: string
    }
  }
}

const trustCards = [
  { label: 'Customer View', value: '360', icon: UsersRound, tone: 'text-[var(--crm-primary)]' },
  { label: 'Activity', value: 'Ready', icon: Activity, tone: 'text-emerald-200' },
  { label: 'Insights', value: 'Reports', icon: BarChart3, tone: 'text-amber-200' },
]

export function LoginPage() {
  const [introDone, setIntroDone] = useState(
    () => sessionStorage.getItem('crm-intro-seen') === 'true',
  )
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (!introDone) {
    return <PreLoginIntro onDone={() => setIntroDone(true)} />
  }

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
          passwordChangeRequired: response.passwordChangeRequired,
        }),
      )

      window.location.href = response.passwordChangeRequired ? '/change-password' : '/dashboard'
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
    <main className="relative min-h-screen overflow-hidden bg-[var(--crm-bg)] text-[var(--crm-text)]">
      <img
        src={bgCommandCenter}
        alt=""
        className="absolute inset-0 h-full w-full object-cover opacity-10"
      />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(109,93,251,0.18),transparent_30rem),radial-gradient(circle_at_70%_10%,rgba(56,189,248,0.14),transparent_26rem),linear-gradient(120deg,rgba(248,250,252,0.92),rgba(246,247,251,0.76))]" />

      <section className="relative z-10 grid min-h-screen lg:grid-cols-[1.12fr_0.88fr]">
        <div className="hidden flex-col justify-between p-10 lg:flex xl:p-14">
          <motion.div
            className="flex items-center gap-3"
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.45 }}
          >
            <div className="relative flex h-12 w-12 items-center justify-center overflow-hidden rounded-2xl bg-white ring-1 ring-violet-200 shadow-[0_16px_36px_rgba(109,93,251,0.16)]">
              <img src={tadamunLogo} alt="Tadamun logo" className="h-10 w-10" />
              <span className="absolute -right-1 -top-1 h-3 w-3 rounded-full bg-emerald-400 shadow-[0_0_18px_rgba(2,245,161,0.9)]" />
            </div>
            <div>
              <h1 className="text-lg font-semibold text-[var(--crm-text)]">Tadamun</h1>
              <p className="text-sm text-[var(--crm-text-muted)]">Sales command center</p>
            </div>
          </motion.div>

          <motion.div
            className="max-w-3xl"
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.55, delay: 0.08 }}
          >
            <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-violet-300/30 bg-violet-500/10 px-3 py-1.5 text-sm font-semibold text-[var(--crm-accent-text)] shadow-[0_14px_32px_rgba(109,93,251,0.12)]">
              <ShieldCheck size={16} />
              Secure role-based Tadamun workspace
            </div>

            <h2 className="max-w-3xl text-5xl font-semibold leading-tight tracking-normal xl:text-6xl">
              Run customers, leads, tasks, and reports from one premium system.
            </h2>

            <p className="mt-5 max-w-2xl text-base leading-7 text-[var(--crm-text-muted)]">
              A focused operating center for sales teams, managers, support staff, and business owners who need clarity, control, and speed.
            </p>

            <div className="mt-8 grid max-w-2xl grid-cols-3 gap-3">
              {trustCards.map((card, index) => {
                const Icon = card.icon

                return (
                  <motion.div
                    key={card.label}
                    className="rounded-3xl border border-[var(--crm-border)] bg-[var(--crm-surface-glass)] p-4 shadow-[var(--crm-shadow-card)] backdrop-blur-xl"
                    initial={{ opacity: 0, y: 16 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.35, delay: 0.18 + index * 0.08 }}
                  >
                    <Icon className={card.tone} size={24} />
                    <p className="mt-4 text-2xl font-semibold">{card.value}</p>
                    <p className="mt-1 text-xs font-medium uppercase tracking-wide text-[var(--crm-text-muted)]">
                      {card.label}
                    </p>
                  </motion.div>
                )
              })}
            </div>
          </motion.div>

          <motion.div
            className="rounded-[2rem] border border-[var(--crm-border)] bg-[var(--crm-surface-glass)] p-5 shadow-[var(--crm-shadow-card)] backdrop-blur-xl"
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.45, delay: 0.28 }}
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold">Workspace</p>
                <p className="mt-1 text-sm text-[var(--crm-text-muted)]">Core CRM modules are ready</p>
              </div>
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-violet-500/10 text-[var(--crm-primary)] ring-1 ring-violet-300/20">
                <Database size={22} />
              </div>
            </div>

            <div className="mt-5 h-2 overflow-hidden rounded-full bg-violet-500/10">
              <div className="h-full w-3/4 animate-pulse rounded-full bg-[var(--crm-brand-gradient)]" />
            </div>
          </motion.div>
        </div>

        <div className="flex min-h-screen items-center justify-center px-4 py-10 sm:px-6 lg:px-10">
          <motion.section
            className="w-full max-w-md rounded-[2rem] border border-[var(--crm-border)] bg-[var(--crm-surface-glass)] p-6 text-[var(--crm-text)] shadow-[var(--crm-shadow-soft)] backdrop-blur-xl sm:p-8"
            initial={{ opacity: 0, scale: 0.97, y: 18 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 0.45, ease: 'easeOut' }}
          >
            <div className="mb-8">
              <div className="mb-5 flex h-14 w-14 items-center justify-center overflow-hidden rounded-2xl bg-white text-white shadow-lg shadow-slate-950/25 ring-1 ring-slate-200">
                <img src={tadamunLogo} alt="Tadamun logo" className="h-11 w-11" />
              </div>

              <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-violet-500/10 px-3 py-1 text-xs font-semibold text-[var(--crm-accent-text)]">
                <Sparkles size={14} />
                Premium access
              </div>

              <h2 className="text-3xl font-semibold tracking-normal">Sign in to Tadamun</h2>
              <p className="mt-2 text-sm leading-6 text-slate-500">
                Enter your account details to continue.
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="mb-2 block text-sm font-semibold text-[var(--crm-text)]">Email</label>
                <div className="relative">
                  <Mail
                    size={18}
                    className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                  />
                  <input
                    className="crm-focus h-12 w-full rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] pl-10 pr-3 text-sm text-[var(--crm-text)] shadow-sm transition focus:border-[var(--crm-primary)]"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    type="email"
                    autoComplete="email"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="mb-2 block text-sm font-semibold text-[var(--crm-text)]">Password</label>
                <div className="relative">
                  <LockKeyhole
                    size={18}
                    className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                  />
                  <input
                    className="crm-focus h-12 w-full rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] pl-10 pr-11 text-sm text-[var(--crm-text)] shadow-sm transition focus:border-[var(--crm-primary)]"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((value) => !value)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <div className="flex justify-end">
                <button
                  type="button"
                  onClick={() => window.location.assign('/forgot-password')}
                  className="text-sm font-semibold text-[var(--crm-accent-text)] transition hover:text-[var(--crm-primary)]"
                >
                  Forgot password?
                </button>
              </div>

              {error && (
                <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
                  {error}
                </div>
              )}

              <button
                className={cn(
                  'crm-primary-action group flex h-12 w-full items-center justify-center gap-2 rounded-2xl px-4 text-sm font-semibold transition hover:-translate-y-0.5 disabled:translate-y-0 disabled:cursor-not-allowed',
                )}
                disabled={loading}
              >
                {loading ? 'Signing in...' : 'Sign in'}
                <ArrowRight size={18} className="transition group-hover:translate-x-1" />
              </button>
            </form>
          </motion.section>
        </div>
      </section>
    </main>
  )
}
