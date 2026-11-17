// Cliente del backend. Siempre lanza ApiError con un `message` apto para mostrar al usuario.
const BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

export class ApiError extends Error {
  constructor(message, { code = 'UNKNOWN', status = 0 } = {}) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

async function request(path, { signal } = {}) {
  let res
  try {
    res = await fetch(`${BASE}${path}`, { headers: { Accept: 'application/json' }, signal })
  } catch (err) {
    if (err?.name === 'AbortError') throw err
    throw new ApiError(
      'No pudimos conectar con el servidor. Revisa tu conexión a internet e inténtalo de nuevo.',
      { code: 'NETWORK_ERROR' },
    )
  }

  let body = null
  try {
    body = await res.json()
  } catch {
    /* respuesta sin JSON */
  }

  if (!res.ok) {
    throw new ApiError(body?.message || `Ocurrió un error inesperado (HTTP ${res.status}).`, {
      code: body?.code || 'UNKNOWN',
      status: res.status,
    })
  }
  return body
}

export const getContribuyente = (ruc, opts) =>
  request(`/api/v1/contribuyentes/${encodeURIComponent(ruc)}`, opts)

export const getPuntosLicencia = (cedula, opts) =>
  request(`/api/v1/licencias/${encodeURIComponent(cedula)}/puntos`, opts)

export const getVehiculo = (placa, opts) =>
  request(`/api/v1/vehiculos/${encodeURIComponent(placa)}`, opts)
