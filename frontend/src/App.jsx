import { useCallback, useEffect, useRef, useState } from 'react'
import { RotateCcw, ShieldCheck } from 'lucide-react'
import StepIndicator from './components/StepIndicator.jsx'
import VerificationStep from './components/VerificationStep.jsx'
import ContribuyenteCard from './components/ContribuyenteCard.jsx'
import LicensePointsCard from './components/LicensePointsCard.jsx'
import VehicleStep from './components/VehicleStep.jsx'
import { getContribuyente, getPuntosLicencia, getVehiculo } from './lib/api.js'

const INITIAL = {
  email: '',
  contribuyente: null,
  verifying: false,
  verifyError: '',
  puntos: { status: 'idle' }, // idle | loading | ok | error
  vehiculo: null,
  vehLoading: false,
  vehError: '',
}

export default function App() {
  const [s, setS] = useState(INITIAL)
  const [formKey, setFormKey] = useState(0) // remonta los formularios en "Nueva Consulta"
  const abort = useRef(new Set())

  // Cancela peticiones en vuelo (al reiniciar o desmontar)
  const cancelAll = useCallback(() => {
    abort.current.forEach((c) => c.abort())
    abort.current.clear()
  }, [])
  useEffect(() => cancelAll, [cancelAll])

  function track() {
    const c = new AbortController()
    abort.current.add(c)
    return c
  }

  const loadPuntos = useCallback(async (ruc) => {
    const c = track()
    setS((p) => ({ ...p, puntos: { status: 'loading' } }))
    try {
      const data = await getPuntosLicencia(ruc.slice(0, 10), { signal: c.signal })
      setS((p) => ({ ...p, puntos: { status: 'ok', data } }))
    } catch (err) {
      if (err.name === 'AbortError') return
      setS((p) => ({ ...p, puntos: { status: 'error', error: err.message } }))
    } finally {
      abort.current.delete(c)
    }
  }, [])

  async function handleVerify({ email, ruc }) {
    const c = track()
    setS((p) => ({ ...p, verifying: true, verifyError: '' }))
    try {
      const contribuyente = await getContribuyente(ruc, { signal: c.signal })
      setS((p) => ({ ...p, email, contribuyente, verifying: false }))
      loadPuntos(contribuyente.ruc) // Paso 2: automático, sin bloquear el paso 3
    } catch (err) {
      if (err.name === 'AbortError') return
      setS((p) => ({ ...p, verifying: false, verifyError: err.message }))
    } finally {
      abort.current.delete(c)
    }
  }

  async function handleVehiculo(placa) {
    const c = track()
    setS((p) => ({ ...p, vehLoading: true, vehError: '', vehiculo: null }))
    try {
      const vehiculo = await getVehiculo(placa, { signal: c.signal })
      setS((p) => ({ ...p, vehiculo, vehLoading: false }))
    } catch (err) {
      if (err.name === 'AbortError') return
      setS((p) => ({ ...p, vehLoading: false, vehError: err.message }))
    } finally {
      abort.current.delete(c)
    }
  }

  function reset() {
    cancelAll()
    setS(INITIAL)
    setFormKey((k) => k + 1)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const verified = Boolean(s.contribuyente)
  const puntosDone = s.puntos.status === 'ok' || s.puntos.status === 'error'
  const statuses = [
    verified ? 'done' : 'current',
    !verified ? 'pending' : puntosDone ? 'done' : 'current',
    s.vehiculo ? 'done' : verified && puntosDone ? 'current' : 'pending',
  ]

  return (
    <div className="mx-auto w-full max-w-2xl px-4 pb-16 pt-8 sm:pt-12">
      <header className="mb-8 flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-900 text-emerald-400">
            <ShieldCheck className="h-6 w-6" aria-hidden="true" />
          </span>
          <div>
            <h1 className="text-xl font-bold leading-tight tracking-tight text-slate-900">Consulta SRI y ANT</h1>
            <p className="text-sm text-slate-500">Contribuyente, puntos de licencia y vehículo</p>
          </div>
        </div>
        {verified && (
          <button
            type="button"
            onClick={reset}
            className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-800 transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-slate-200"
          >
            <RotateCcw className="h-4 w-4" aria-hidden="true" />
            Nueva Consulta
          </button>
        )}
      </header>

      <div className="mb-6">
        <StepIndicator statuses={statuses} />
      </div>

      <main className="space-y-5">
        {!verified && (
          <VerificationStep key={formKey} loading={s.verifying} error={s.verifyError} onSubmit={handleVerify} />
        )}

        {verified && (
          <>
            <ContribuyenteCard data={s.contribuyente} email={s.email} />
            <LicensePointsCard state={s.puntos} onRetry={() => loadPuntos(s.contribuyente.ruc)} />
            <VehicleStep
              key={formKey}
              loading={s.vehLoading}
              error={s.vehError}
              vehiculo={s.vehiculo}
              onSubmit={handleVehiculo}
            />
          </>
        )}
      </main>

      <footer className="mt-10 text-center text-xs text-slate-400">
        Los datos provienen de servicios públicos del SRI y la ANT y pueden no estar disponibles en ciertos momentos.
      </footer>
    </div>
  )
}
