import { useState } from 'react'
import { IdCard, Mail, ShieldCheck } from 'lucide-react'
import { Card, CardTitle, ErrorAlert, Field, PrimaryButton, inputClass } from './ui.jsx'
import { validateEmail, validateRuc } from '../lib/validators.js'

export default function VerificationStep({ loading, error, onSubmit }) {
  const [email, setEmail] = useState('')
  const [ruc, setRuc] = useState('')
  const [touched, setTouched] = useState({ email: false, ruc: false })

  const emailError = touched.email ? validateEmail(email) : ''
  const rucError = touched.ruc ? validateRuc(ruc) : ''

  function handleSubmit(e) {
    e.preventDefault()
    setTouched({ email: true, ruc: true })
    if (validateEmail(email) || validateRuc(ruc)) return
    onSubmit({ email: email.trim(), ruc })
  }

  return (
    <Card>
      <CardTitle
        icon={ShieldCheck}
        step={1}
        title="Verifica tu RUC de persona natural"
        subtitle="Con tu correo y tu RUC consultamos tu información en el SRI."
      />
      <form onSubmit={handleSubmit} noValidate className="space-y-5">
        <Field id="email" label="Correo electrónico" error={emailError}>
          <div className="relative">
            <Mail className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" aria-hidden="true" />
            <input
              id="email"
              type="email"
              autoComplete="email"
              inputMode="email"
              placeholder="nombre@correo.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, email: true }))}
              aria-invalid={Boolean(emailError)}
              aria-describedby={emailError ? 'email-error' : undefined}
              className={`${inputClass(emailError)} pl-11`}
            />
          </div>
        </Field>

        <Field
          id="ruc"
          label="RUC de persona natural"
          error={rucError}
          hint={`13 dígitos, termina en 001 · ${ruc.length}/13`}
        >
          <div className="relative">
            <IdCard className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" aria-hidden="true" />
            <input
              id="ruc"
              type="text"
              inputMode="numeric"
              autoComplete="off"
              maxLength={13}
              placeholder="1712345678001"
              value={ruc}
              onChange={(e) => setRuc(e.target.value.replace(/\D/g, '').slice(0, 13))}
              onBlur={() => setTouched((t) => ({ ...t, ruc: true }))}
              aria-invalid={Boolean(rucError)}
              aria-describedby={rucError ? 'ruc-error' : 'ruc-hint'}
              className={`${inputClass(rucError)} pl-11 font-mono tracking-wide`}
            />
          </div>
        </Field>

        <ErrorAlert message={error} />

        <PrimaryButton type="submit" loading={loading} loadingLabel="Verificando en el SRI…" icon={ShieldCheck}>
          Verificar Contribuyente
        </PrimaryButton>
      </form>
    </Card>
  )
}
