import { useState } from 'react'
import { Car, Search } from 'lucide-react'
import { Card, CardTitle, DataItem, ErrorAlert, Field, PrimaryButton, inputClass } from './ui.jsx'
import { normalizePlaca, validatePlaca } from '../lib/validators.js'
import { formatDate, titleCase } from '../lib/format.js'

export default function VehicleStep({ loading, error, vehiculo, onSubmit }) {
  const [placa, setPlaca] = useState('')
  const [touched, setTouched] = useState(false)
  const placaError = touched ? validatePlaca(placa) : ''

  function handleSubmit(e) {
    e.preventDefault()
    setTouched(true)
    if (validatePlaca(placa)) return
    onSubmit(placa)
  }

  return (
    <Card>
      <CardTitle
        icon={Car}
        step={3}
        title="Consulta tu vehículo"
        subtitle="Ingresa la matrícula para ver la ficha técnica registrada en el SRI."
      />

      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4 sm:flex-row sm:items-end">
        <div className="flex-1">
          <Field id="placa" label="Matrícula / Placa" error={placaError} hint="Ejemplo: ABC1234">
            <input
              id="placa"
              type="text"
              autoComplete="off"
              autoCapitalize="characters"
              maxLength={8}
              placeholder="ABC1234"
              value={placa}
              onChange={(e) => setPlaca(normalizePlaca(e.target.value).slice(0, 7))}
              onBlur={() => setTouched(true)}
              aria-invalid={Boolean(placaError)}
              aria-describedby={placaError ? 'placa-error' : 'placa-hint'}
              className={`${inputClass(placaError)} font-mono uppercase tracking-widest`}
            />
          </Field>
        </div>
        <PrimaryButton type="submit" loading={loading} loadingLabel="Consultando…" icon={Search}>
          Consultar Vehículo
        </PrimaryButton>
      </form>

      {error && (
        <div className="mt-5">
          <ErrorAlert message={error} />
        </div>
      )}

      {loading && !vehiculo && (
        <p className="sr-only" role="status">Consultando vehículo en el SRI…</p>
      )}

      {vehiculo && (
        <div className="rise mt-6 border-t border-slate-100 pt-5">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="font-semibold text-slate-900">Ficha técnica</h3>
            <span className="rounded-lg border-2 border-slate-900 bg-white px-3 py-0.5 font-mono text-sm font-bold tracking-widest text-slate-900">
              {vehiculo.placa}
            </span>
          </div>
          <dl className="grid gap-3 sm:grid-cols-2">
            <DataItem label="Marca" value={titleCase(vehiculo.marca)} />
            <DataItem label="Modelo" value={titleCase(vehiculo.modelo)} />
            <DataItem label="Año" value={vehiculo.anio ?? '—'} />
            <DataItem label="Cilindraje" value={vehiculo.cilindraje != null ? `${vehiculo.cilindraje} cc` : '—'} />
            <DataItem label="Clase" value={titleCase(vehiculo.clase)} />
            {vehiculo.servicio && <DataItem label="Servicio" value={titleCase(vehiculo.servicio)} />}
            <DataItem label="Última matrícula" value={formatDate(vehiculo.fechaUltimaMatricula)} />
            <DataItem label="Estado" value={titleCase(vehiculo.estado)} />
          </dl>
        </div>
      )}
    </Card>
  )
}
