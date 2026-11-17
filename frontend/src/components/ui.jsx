import { AlertTriangle, CheckCircle2, Loader2, XCircle } from 'lucide-react'

export function Spinner({ label, className = '' }) {
  return (
    <span role="status" className={`inline-flex items-center gap-2 text-slate-600 ${className}`}>
      <Loader2 className="h-5 w-5 animate-spin text-emerald-600" aria-hidden="true" />
      {label && <span className="text-sm">{label}</span>}
    </span>
  )
}

export function Card({ children, className = '' }) {
  return (
    <section className={`rise rounded-2xl border border-slate-200 bg-white p-6 shadow-sm shadow-slate-200/60 ${className}`}>
      {children}
    </section>
  )
}

export function CardTitle({ icon: Icon, step, title, subtitle, right }) {
  return (
    <header className="mb-5 flex items-start justify-between gap-4">
      <div className="flex items-start gap-3">
        <span className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-slate-900 text-white">
          <Icon className="h-5 w-5" aria-hidden="true" />
        </span>
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-emerald-700">Paso {step}</p>
          <h2 className="text-lg font-semibold leading-tight text-slate-900">{title}</h2>
          {subtitle && <p className="mt-0.5 text-sm text-slate-500">{subtitle}</p>}
        </div>
      </div>
      {right}
    </header>
  )
}

export function ErrorAlert({ message, onRetry, retryLabel = 'Reintentar' }) {
  if (!message) return null
  return (
    <div role="alert" className="flex items-start gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-900">
      <XCircle className="mt-0.5 h-5 w-5 shrink-0 text-rose-600" aria-hidden="true" />
      <div className="text-sm">
        <p>{message}</p>
        {onRetry && (
          <button type="button" onClick={onRetry} className="mt-2 font-semibold underline underline-offset-2 hover:text-rose-700">
            {retryLabel}
          </button>
        )}
      </div>
    </div>
  )
}

export function WarningBanner({ title, children }) {
  return (
    <div role="alert" className="flex items-start gap-3 rounded-xl border border-amber-300 bg-amber-50 p-4 text-amber-950">
      <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" aria-hidden="true" />
      <div className="text-sm">
        {title && <p className="font-semibold">{title}</p>}
        <p>{children}</p>
      </div>
    </div>
  )
}

export function LiveBadge({ children }) {
  return (
    <span className="inline-flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-800">
      <CheckCircle2 className="h-3.5 w-3.5" aria-hidden="true" />
      {children}
    </span>
  )
}

export function Field({ id, label, hint, error, children }) {
  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-slate-700">
        {label}
      </label>
      {children}
      {error ? (
        <p id={`${id}-error`} className="mt-1.5 text-sm text-rose-700">{error}</p>
      ) : (
        hint && <p id={`${id}-hint`} className="mt-1.5 text-sm text-slate-500">{hint}</p>
      )}
    </div>
  )
}

export const inputClass = (hasError) =>
  `w-full rounded-xl border bg-white px-3.5 py-2.5 text-slate-900 placeholder:text-slate-400 outline-none transition
   focus:ring-4 ${hasError ? 'border-rose-400 focus:border-rose-500 focus:ring-rose-100' : 'border-slate-300 focus:border-emerald-500 focus:ring-emerald-100'}`

export function PrimaryButton({ loading, loadingLabel, children, icon: Icon, ...props }) {
  return (
    <button
      {...props}
      disabled={loading || props.disabled}
      className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-slate-900 px-5 py-3 font-semibold text-white transition hover:bg-slate-800 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-slate-300 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
    >
      {loading ? <Loader2 className="h-5 w-5 animate-spin" aria-hidden="true" /> : Icon && <Icon className="h-5 w-5" aria-hidden="true" />}
      {loading ? loadingLabel : children}
    </button>
  )
}

export function DataItem({ label, value }) {
  return (
    <div className="rounded-xl bg-slate-50 px-4 py-3">
      <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</dt>
      <dd className="mt-0.5 break-words font-medium text-slate-900">{value ?? '—'}</dd>
    </div>
  )
}
