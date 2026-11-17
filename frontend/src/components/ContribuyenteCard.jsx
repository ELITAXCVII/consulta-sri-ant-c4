import { BadgeCheck, UserRound } from 'lucide-react'
import { Card, CardTitle, DataItem } from './ui.jsx'
import { formatDate, titleCase } from '../lib/format.js'

export default function ContribuyenteCard({ data, email }) {
  const activo = /activo/i.test(data.estado || '')
  return (
    <Card>
      <CardTitle
        icon={UserRound}
        step={2}
        title="Contribuyente verificado"
        subtitle={email ? `Consulta asociada a ${email}` : undefined}
        right={
          <span
            className={`inline-flex shrink-0 items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ${
              activo ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-slate-100 text-slate-700 border border-slate-200'
            }`}
          >
            <BadgeCheck className="h-3.5 w-3.5" aria-hidden="true" />
            {data.estado ? titleCase(data.estado) : 'Verificado'}
          </span>
        }
      />
      <dl className="grid gap-3 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <DataItem label="Nombre / Razón social" value={data.razonSocial} />
        </div>
        <DataItem label="RUC" value={<span className="font-mono">{data.ruc}</span>} />
        <DataItem label="Tipo de contribuyente" value={titleCase(data.tipoContribuyente)} />
        <div className="sm:col-span-2">
          <DataItem label="Actividad económica" value={data.actividadEconomica ? titleCase(data.actividadEconomica) : '—'} />
        </div>
        {data.fechaInicioActividades && (
          <DataItem label="Inicio de actividades" value={formatDate(data.fechaInicioActividades)} />
        )}
        {data.regimen && <DataItem label="Régimen" value={titleCase(data.regimen)} />}
      </dl>
    </Card>
  )
}
