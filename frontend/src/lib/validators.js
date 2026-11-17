// Validaciones del lado cliente (el backend vuelve a validar todo).
export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/
export const RUC_RE = /^\d{10}001$/
const PLACA_AUTO_RE = /^[A-Z]{3}\d{3,4}$/
const PLACA_MOTO_RE = /^[A-Z]{2}\d{3}[A-Z]$/

export function validateEmail(value) {
  if (!value.trim()) return 'Ingresa tu correo electrónico.'
  if (!EMAIL_RE.test(value.trim())) return 'Revisa el correo: debería verse como nombre@dominio.com.'
  return ''
}

export function validateRuc(value) {
  if (!value) return 'Ingresa el RUC de persona natural.'
  if (!/^\d+$/.test(value)) return 'El RUC solo puede contener números.'
  if (value.length !== 13) return `El RUC debe tener 13 dígitos (llevas ${value.length}).`
  if (!RUC_RE.test(value)) return 'El RUC de una persona natural termina en 001.'
  return ''
}

export function normalizePlaca(value) {
  return value.toUpperCase().replace(/[^A-Z0-9]/g, '')
}

export function validatePlaca(value) {
  if (!value) return 'Ingresa la placa del vehículo.'
  if (!PLACA_AUTO_RE.test(value) && !PLACA_MOTO_RE.test(value)) {
    return 'Formato de placa no válido. Usa 3 letras y 3 o 4 números, por ejemplo ABC1234.'
  }
  return ''
}
