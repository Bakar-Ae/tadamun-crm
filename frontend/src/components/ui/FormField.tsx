import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";

type FieldShellProps = {
  label: string;
  error?: string;
  children: ReactNode;
};

function FieldShell({ label, error, children }: FieldShellProps) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-semibold text-[var(--crm-text)]">
        {label}
      </span>
      {children}
      {error && (
        <span className="mt-1 block text-xs font-semibold text-[var(--crm-danger-text)]">
          {error}
        </span>
      )}
    </label>
  );
}

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  error?: string;
};

export function TextField({ label, error, className, ...props }: TextFieldProps) {
  return (
    <FieldShell label={label} error={error}>
      <input
        {...props}
        className={`crm-focus h-11 w-full rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)] shadow-sm transition placeholder:text-[var(--crm-text-muted)] focus:border-[var(--crm-primary)] ${className ?? ""}`}
      />
    </FieldShell>
  );
}

type TextAreaFieldProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  label: string;
  error?: string;
};

export function TextAreaField({ label, error, className, ...props }: TextAreaFieldProps) {
  return (
    <FieldShell label={label} error={error}>
      <textarea
        {...props}
        className={`crm-focus min-h-24 w-full rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 py-2 text-sm text-[var(--crm-text)] shadow-sm transition placeholder:text-[var(--crm-text-muted)] focus:border-[var(--crm-primary)] ${className ?? ""}`}
      />
    </FieldShell>
  );
}

type SelectFieldProps = SelectHTMLAttributes<HTMLSelectElement> & {
  label: string;
  error?: string;
};

export function SelectField({ label, error, className, children, ...props }: SelectFieldProps) {
  return (
    <FieldShell label={label} error={error}>
      <select
        {...props}
        className={`crm-focus h-11 w-full rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)] shadow-sm transition focus:border-[var(--crm-primary)] ${className ?? ""}`}
      >
        {children}
      </select>
    </FieldShell>
  );
}
