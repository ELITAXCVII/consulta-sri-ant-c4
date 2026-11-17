const TZ = 'America/Guayaquil'

const dateTime = new Intl.DateTimeFormat('es-EC', { dateStyle: 'medium', timeStyle: 'short', timeZone: TZ })
const dateOnly = new Intl.DateTimeFormat('es-EC', { dateStyle: 'long' })

export function formatDateTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? '—' : dateTime.format(d)
}

// Fechas "yyyy-MM-dd" sin desfase de zona horaria
export function formatDate(ymd) {
  if (!ymd) return '—'
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(ymd)
  if (!m) return ymd
  return dateOnly.format(new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3])))
}

const MINUSCULAS = new Set(['de', 'del', 'la', 'las', 'el', 'los', 'y', 'e', 'en', 'para', 'por', 'con', 'a'])

export function titleCase(text) {
  if (!text) return '—'
  return text
    .toLowerCase()
    .split(/(\s+)/)
    .map((w, i) => (i > 0 && MINUSCULAS.has(w) ? w : w.replace(/^\S/, (c) => c.toUpperCase())))
    .join('')
}
