import type {
  ComponentPropsWithoutRef,
  ReactNode,
} from 'react'

type BaseFieldProps = {
  label: string
  error?: string
  helperText?: string
}

function FieldMessage({ error, helperText }: Pick<BaseFieldProps, 'error' | 'helperText'>) {
  if (error) {
    return <p className="mt-1 text-xs font-medium text-red-600">{error}</p>
  }

  if (helperText) {
    return <p className="mt-1 text-xs text-[var(--crm-text-muted)]">{helperText}</p>
  }

  return null
}

function FieldLabel({ label, required }: Pick<BaseFieldProps, 'label'> & { required?: boolean }) {
  return (
    <span className="text-xs font-semibold uppercase tracking-wide text-[var(--crm-text-muted)]">
      {label}
      {required ? <span className="ml-1 text-red-600">*</span> : null}
    </span>
  )
}

type TextFieldProps = BaseFieldProps & ComponentPropsWithoutRef<'input'>

export function TextField({ label, error, helperText, required, className, ...props }: TextFieldProps) {
  return (
    <label className="block">
      <FieldLabel label={label} required={required} />
      <input
        required={required}
        aria-invalid={Boolean(error)}
        className={`crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)] outline-none transition placeholder:text-[var(--crm-text-muted)] ${
          error ? 'border-red-400 ring-2 ring-red-500/10' : ''
        } ${className ?? ''}`}
        {...props}
      />
      <FieldMessage error={error} helperText={helperText} />
    </label>
  )
}

type TextAreaFieldProps = BaseFieldProps & ComponentPropsWithoutRef<'textarea'>

export function TextAreaField({
  label,
  error,
  helperText,
  required,
  className,
  ...props
}: TextAreaFieldProps) {
  return (
    <label className="block">
      <FieldLabel label={label} required={required} />
      <textarea
        required={required}
        aria-invalid={Boolean(error)}
        className={`crm-focus mt-1 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 py-3 text-sm text-[var(--crm-text)] outline-none transition placeholder:text-[var(--crm-text-muted)] ${
          error ? 'border-red-400 ring-2 ring-red-500/10' : ''
        } ${className ?? ''}`}
        {...props}
      />
      <FieldMessage error={error} helperText={helperText} />
    </label>
  )
}

type SelectFieldProps = BaseFieldProps & ComponentPropsWithoutRef<'select'> & {
  children: ReactNode
}

export function SelectField({
  label,
  error,
  helperText,
  required,
  className,
  children,
  ...props
}: SelectFieldProps) {
  return (
    <label className="block">
      <FieldLabel label={label} required={required} />
      <select
        required={required}
        aria-invalid={Boolean(error)}
        className={`crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)] outline-none transition ${
          error ? 'border-red-400 ring-2 ring-red-500/10' : ''
        } ${className ?? ''}`}
        {...props}
      >
        {children}
      </select>
      <FieldMessage error={error} helperText={helperText} />
    </label>
  )
}
