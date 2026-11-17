import { Clock3, IdCard } from 'lucide-react'
import { Card, CardTitle, ErrorAlert, LiveBadge, Spinner, WarningBanner } from './ui.jsx'
import { formatDateTime } from '../lib/format.js'

const MAX_PUNTOS = 30

function PointsMeter({ puntos, stale }) {
  const pct = Math.max(0, Math.min(100, (puntos / MAX_PUNTOS) * 100))
  const tone = stale ? 'bg-amber-500' : pct >= 60 ? 'bg-emerald-500' : pct >= 30 ? 'bg-amber-500' : 'bg-rose-500'
  return (
    <div className="mt-3" aria-hidden="true">
      <div className="h-2.5 w-full overflow-hidden rounded-full bg-slate-100">
        <div className={`h-full rounded-full transition-all duration-700 ${tone}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}

/** state: { status: 'loading' | 'ok' | 'error', data?, error? } */
export default function LicensePointsCard({ state, onRetry }) {
  const { status, data, error } = state
  const stale = status === 'ok' && data.origen === 'CACHE_RESPALDO'

  return (
    <Card>
      <CardTitle
        icon={IdCard}
        step={2}
        title="Puntos de la licencia (ANT)"
        subtitle="Consultamos automáticamente con los 10 primeros dígitos de tu RUC."
        right={
          status === 'ok' && !stale ? (
            <LiveBadge>{data.origen === 'CACHE_FRESCA' ? 'Dato reciente' : 'Dato en vivo · ANT'}</LiveBadge>
          ) : null
        }
      />

      {status === 'loading' && <Spinner label="Consultando puntos de licencia en la ANT…" className="py-4" />}

      {status === 'error' && <ErrorAlert message={error} onRetry={onRetry} retryLabel="Volver a consultar la ANT" />}

      {status === 'ok' && (
        <div className="space-y-4">
          {stale && (
            <WarningBanner title="Mostrando información guardada">
              {data.mensaje}
            </WarningBanner>
          )}

          <div className="rounded-xl bg-slate-50 p-5">
            {data.nombreConductor && (
              <p className="mb-3 text-sm text-slate-500">
                Conductor: <span className="font-medium text-slate-900">{data.nombreConductor}</span>
              </p>
            )}
            <div className="flex items-end gap-2">
              <span className={`text-5xl font-bold leading-none tabular-nums ${stale ? 'text-amber-700' : 'text-slate-900'}`}>
                {data.puntos}
              </span>
              <span className="pb-1 text-slate-500">/ {MAX_PUNTOS} puntos</span>
            </div>
            <PointsMeter puntos={data.puntos} stale={stale} />
          </div>

          <p className="flex items-center gap-1.5 text-sm text-slate-500">
            <Clock3 className="h-4 w-4" aria-hidden="true" />
            {stale
              ? `Última actualización de la ANT: ${formatDateTime(data.fechaConsultaOriginal)}`
              : `Consultado el ${formatDateTime(data.fechaConsulta)}`}
          </p>
        </div>
      )}
    </Card>
  )
}
