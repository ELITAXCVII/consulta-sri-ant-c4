import { Check } from 'lucide-react'

const STEPS = ['Verificar contribuyente', 'Licencia ANT', 'Consultar vehículo']

/** status: 'done' | 'current' | 'pending' por cada paso */
export default function StepIndicator({ statuses }) {
  return (
    <ol className="flex items-center gap-2 sm:gap-3" aria-label="Progreso">
      {STEPS.map((label, i) => {
        const status = statuses[i]
        return (
          <li key={label} className="flex flex-1 items-center gap-2" aria-current={status === 'current' ? 'step' : undefined}>
            <span
              className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold transition
                ${status === 'done' ? 'bg-emerald-600 text-white' : status === 'current' ? 'bg-slate-900 text-white ring-4 ring-slate-300/60' : 'bg-slate-200 text-slate-500'}`}
            >
              {status === 'done' ? <Check className="h-4 w-4" aria-hidden="true" /> : i + 1}
            </span>
            <span className={`hidden text-sm sm:inline ${status === 'pending' ? 'text-slate-400' : 'font-medium text-slate-800'}`}>{label}</span>
            {i < STEPS.length - 1 && <span className={`h-px flex-1 ${status === 'done' ? 'bg-emerald-400' : 'bg-slate-200'}`} />}
          </li>
        )
      })}
    </ol>
  )
}
