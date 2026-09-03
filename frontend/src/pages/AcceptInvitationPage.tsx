import { useEffect, useState, type FormEvent } from 'react'
import { motion } from 'framer-motion'
import {
  AlertTriangle,
  CheckCircle2,
  LoaderCircle,
  ShieldCheck,
} from 'lucide-react'
import { Link, useSearchParams } from 'react-router'
import { TextField } from '../components/ui/FormField'
import { formatDateTime, formatRole } from '../lib/formatters'
import {
  acceptOrganizationInvitation,
  previewOrganizationInvitation,
  type OrganizationInvitationPreview,
} from '../services/organizationInvitationService'

type PageStatus = 'loading' | 'ready' | 'error' | 'success'

type ApiError = {
  response?: {
    data?: {
      message?: string
    }
  }
}

function getErrorMessage(error: unknown, fallback: string) {
  return (error as ApiError).response?.data?.message ?? fallback
}

export function AcceptInvitationPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')?.trim() ?? ''

  const [status, setStatus] = useState<PageStatus>(
    token ? 'loading' : 'error',
  )
  const [invitation, setInvitation] =
    useState<OrganizationInvitationPreview | null>(null)
  const [fullName, setFullName] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState(
    token ? '' : 'This invitation link is incomplete.',
  )
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!token) {
      return
    }

    let active = true

    previewOrganizationInvitation(token)
      .then((response) => {
        if (!active) return

        setInvitation(response)
        setStatus('ready')
      })
      .catch((requestError) => {
        if (!active) return

        setError(
          getErrorMessage(
            requestError,
            'This invitation is invalid or no longer available.',
          ),
        )
        setStatus('error')
      })

    return () => {
      active = false
    }
  }, [token])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!invitation) return

    if (invitation.requiresAccountCreation) {
      if (!fullName.trim()) {
        setError('Enter your full name.')
        return
      }

      if (password.length < 8) {
        setError('Password must contain at least 8 characters.')
        return
      }

      if (password !== confirmPassword) {
        setError('Passwords do not match.')
        return
      }
    }

    setError('')
    setSubmitting(true)

    try {
      await acceptOrganizationInvitation({
        token,
        ...(invitation.requiresAccountCreation
          ? {
              fullName: fullName.trim(),
              password,
              confirmPassword,
            }
          : {}),
      })

      setStatus('success')
    } catch (requestError) {
      setError(
        getErrorMessage(
          requestError,
          'The invitation could not be accepted.',
        ),
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="grid min-h-dvh place-items-center bg-[var(--crm-bg)] px-4 py-10 text-[var(--crm-text)]">
      <motion.section
        className="w-full max-w-lg rounded-3xl border border-[var(--crm-border)] bg-[var(--crm-surface-glass)] p-6 shadow-[var(--crm-shadow-soft)] sm:p-8"
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, ease: 'easeOut' }}
      >
        <header className="mb-7 flex items-center gap-3 border-b border-[var(--crm-border)] pb-5">
          <div className="grid h-11 w-11 place-items-center rounded-xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)]">
            <ShieldCheck size={22} />
          </div>

          <div>
            <p className="font-semibold">Tadamun</p>
            <p className="text-sm text-[var(--crm-text-muted)]">
              Workspace invitation
            </p>
          </div>
        </header>

        {status === 'loading' && (
          <div
            className="flex min-h-48 flex-col items-center justify-center text-center"
            role="status"
          >
            <LoaderCircle
              className="mb-4 animate-spin text-[var(--crm-primary)]"
              size={30}
            />
            <h1 className="text-xl font-semibold">Checking invitation</h1>
            <p className="mt-2 text-sm text-[var(--crm-text-muted)]">
              Confirming the workspace details.
            </p>
          </div>
        )}

        {status === 'error' && (
          <div className="text-center" role="alert">
            <AlertTriangle
              className="mx-auto text-[var(--crm-danger-text)]"
              size={34}
            />
            <h1 className="mt-4 text-2xl font-semibold">
              Invitation unavailable
            </h1>
            <p className="mt-2 text-sm leading-6 text-[var(--crm-text-muted)]">
              {error}
            </p>
            <Link
              to="/login"
              className="mt-6 inline-flex h-11 items-center justify-center rounded-xl border border-[var(--crm-border)] px-5 text-sm font-semibold"
            >
              Go to sign in
            </Link>
          </div>
        )}

        {status === 'success' && invitation && (
          <div className="text-center" role="status">
            <CheckCircle2
              className="mx-auto text-[var(--crm-success-text)]"
              size={38}
            />
            <h1 className="mt-4 text-2xl font-semibold">
              Invitation accepted
            </h1>
            <p className="mt-2 text-sm leading-6 text-[var(--crm-text-muted)]">
              You can now access {invitation.organizationName}.
            </p>
            <Link
              to="/login"
              className="crm-primary-action mt-6 inline-flex h-11 items-center justify-center rounded-xl px-6 text-sm font-semibold"
            >
              Sign in
            </Link>
          </div>
        )}

        {status === 'ready' && invitation && (
          <>
            <h1 className="text-2xl font-semibold">
              Join {invitation.organizationName}
            </h1>
            <p className="mt-2 text-sm text-[var(--crm-text-muted)]">
              Review your invitation before joining the workspace.
            </p>

            <dl className="my-6 grid gap-3 border-y border-[var(--crm-border)] py-5 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-[var(--crm-text-muted)]">Email</dt>
                <dd className="mt-1 font-medium">{invitation.email}</dd>
              </div>
              <div>
                <dt className="text-[var(--crm-text-muted)]">Role</dt>
                <dd className="mt-1 font-medium">
                  {formatRole(invitation.role)}
                </dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-[var(--crm-text-muted)]">Expires</dt>
                <dd className="mt-1 font-medium">
                  {formatDateTime(invitation.expiresAt)}
                </dd>
              </div>
            </dl>

            <form onSubmit={handleSubmit} className="space-y-4">
              {invitation.requiresAccountCreation && (
                <>
                  <h2 className="font-semibold">Create your account</h2>

                  <TextField
                    label="Full name"
                    value={fullName}
                    onChange={(event) => setFullName(event.target.value)}
                    autoComplete="name"
                    required
                  />

                  <TextField
                    label="Password"
                    type="password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    autoComplete="new-password"
                    helperText="Use at least 8 characters."
                    required
                  />

                  <TextField
                    label="Confirm password"
                    type="password"
                    value={confirmPassword}
                    onChange={(event) =>
                      setConfirmPassword(event.target.value)
                    }
                    autoComplete="new-password"
                    required
                  />
                </>
              )}

              {!invitation.requiresAccountCreation && (
                <p className="text-sm leading-6 text-[var(--crm-text-muted)]">
                  Your existing Tadamun account will be added to this
                  workspace.
                </p>
              )}

              {error && (
                <p
                  role="alert"
                  className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]"
                >
                  {error}
                </p>
              )}

              <button
                className="crm-primary-action flex h-12 w-full items-center justify-center rounded-xl px-4 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-60"
                disabled={submitting}
              >
                {submitting ? 'Accepting...' : 'Accept invitation'}
              </button>
            </form>
          </>
        )}
      </motion.section>
    </main>
  )
}