# Consulta SRI y ANT — contribuyente, vehículo y puntos de licencia

Sistema web full-stack (monorepo) que, de forma guiada en 3 pasos:

1. verifica un **RUC de persona natural** en el SRI,
2. consulta los **puntos de licencia** en la ANT (con caché de respaldo en Redis si la ANT está caída),
3. consulta la **ficha de un vehículo** por placa en el SRI.

```
.
├── backend/            Java 17 · Spring Boot 3 · Maven · Jsoup · Redis (Lettuce)
├── frontend/           React · Vite · Tailwind CSS 4 · Lucide
├── docker-compose.yml  Redis local para desarrollo
└── README.md
```

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17 o superior |
| Maven | 3.9+ |
| Node.js | 18+ (probado con 22) |
| Redis | Upstash (nube) o local con `docker compose up -d` |

## Puesta en marcha

### 1. Redis

**Upstash (recomendado, es el entorno objetivo):** crea una base en <https://console.upstash.com> y copia
*Endpoint*, *Port* y *Password*.

**Local:** `docker compose up -d` y usa `REDIS_HOST=localhost`, `REDIS_PORT=6379`, `REDIS_SSL=false`.

### 2. Backend

```bash
cd backend
cp .env.example .env      # completa REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
mvn spring-boot:run       # http://localhost:8080
```

`application.yml` importa automáticamente `backend/.env` (`spring.config.import: optional:file:.env[.properties]`)
cuando se ejecuta desde la carpeta `backend`. También puedes exportar las variables en el shell.

Compilar y probar: `mvn verify` · JAR ejecutable: `mvn package && java -jar target/api-1.0.0.jar`.

> Si Redis no está accesible el backend **sigue funcionando** (la caché es *best effort*): las consultas en vivo
> responden con normalidad, pero no habrá respaldo si la ANT cae (devolverá 503).

### 3. Frontend

```bash
cd frontend
cp .env.example .env      # en desarrollo puedes dejarlo tal cual
npm install
npm run dev               # http://localhost:5173  (proxy de /api -> localhost:8080)
```

Producción: `npm run build` genera `frontend/dist`. Define `VITE_API_BASE_URL` con la URL pública del backend
**antes** de compilar y agrega ese origen del frontend en `CORS_ALLOWED_ORIGINS` del backend.

## Variables de entorno

### Backend (`backend/.env`)

| Variable | Obligatoria | Por defecto | Descripción |
|---|---|---|---|
| `REDIS_HOST` | sí | `localhost` | Host de Upstash (`xxxx.upstash.io`) |
| `REDIS_PORT` | sí | `6379` | Puerto |
| `REDIS_PASSWORD` | sí (Upstash) | vacío | Contraseña |
| `REDIS_SSL` | no | `true` | TLS. Upstash lo exige; `false` solo para Redis local |
| `PORT` | no | `8080` | Puerto HTTP |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:5173` | Orígenes permitidos, separados por coma |
| `ANT_TIMEOUT_MS` | no | `5000` | Timeout de la consulta a la ANT |
| `SRI_TIMEOUT_MS` | no | `8000` | Timeout de cada consulta al SRI |
| `ANT_FRESH_TTL` | no | `1h` | TTL de la clave de frescura |
| `ANT_BACKUP_TTL` | no | `7d` | TTL del respaldo (ver justificación abajo) |
| `ANT_SERVE_FRESH_CACHE` | no | `false` | Si es `true`, un dato de menos de 1 h se sirve sin llamar a la ANT |
| `SRI_BASE_URL`, `ANT_BASE_URL` | no | URLs oficiales | Sobrescribibles para pruebas |

### Frontend (`frontend/.env`)

| Variable | Descripción |
|---|---|
| `VITE_API_BASE_URL` | URL base del backend en producción (vacía en desarrollo) |
| `VITE_DEV_PROXY_TARGET` | Solo desarrollo: destino del proxy de Vite (por defecto `http://localhost:8080`) |

## API REST (`/api/v1`)

Todos los errores usan el mismo formato:

```json
{ "code": "RUC_NOT_FOUND", "message": "El RUC ingresado no existe en el SRI", "timestamp": "2026-11-17T17:05:11.532Z" }
```

### `GET /api/v1/contribuyentes/{ruc}`

Valida (13 dígitos, termina en `001`) → `existePorNumeroRuc` → `obtenerPorNumerosRuc` → exige persona natural.

| Código | `code` | Cuándo |
|---|---|---|
| 200 | — | RUC, cédula, razón social/nombre, estado, actividad económica, tipo, régimen, fecha de inicio |
| 400 | `INVALID_RUC` | Formato inválido |
| 404 | `RUC_NOT_FOUND` | El SRI responde que no existe |
| 422 | `RUC_NOT_NATURAL_PERSON` | Es sociedad / persona jurídica |
| 502 | `SRI_UNAVAILABLE` | Timeout, error de conexión o HTTP ≠ 2xx del SRI |

La persona natural se detecta por `tipoContribuyente` (contiene `NATURAL`); si el campo no es concluyente se usa la
regla del tercer dígito del RUC (< 6 = natural).

### `GET /api/v1/vehiculos/{placa}`

Acepta `ABC1234`, `ABC0123`, `ABC123` (3 letras + 3–4 dígitos) y motos `AB123C`; normaliza a mayúsculas y quita guiones.

| Código | `code` | Cuándo |
|---|---|---|
| 200 | — | placa, marca, modelo, año, cilindraje, clase, servicio, fecha de última matrícula, estado |
| 400 | `INVALID_PLACA` | Formato inválido |
| 404 | `VEHICLE_NOT_FOUND` | El SRI no tiene datos para la placa |
| 502 | `SRI_UNAVAILABLE` | SRI caído |

### `GET /api/v1/licencias/{cedula}/puntos?placa=XXX`

`{cedula}` acepta 10 dígitos o un RUC de 13 (se toman los 10 primeros); `placa` es opcional.
Consume la ANT con timeout de 5 s y User-Agent de navegador, parsea el HTML con **Jsoup** y aplica el patrón
*stale-while-error*:

| Situación | Respuesta |
|---|---|
| ANT responde bien | `200` · `origen: "ANT_EN_VIVO"`, `esDatoReciente: true`. Se guardan las dos claves de Redis |
| ANT falla (timeout, 5xx, conexión, HTML vacío/sin puntos) **y hay respaldo** | `200` · `origen: "CACHE_RESPALDO"`, `esDatoReciente: false`, `fechaConsultaOriginal`, `mensaje: "El servicio de la ANT no está disponible en este momento. Mostrando el último registro guardado del dd/MM/yyyy HH:mm"` |
| ANT falla **y no hay respaldo** | `503` · `ANT_UNAVAILABLE` |
| Cédula/placa inválida | `400` · `INVALID_CEDULA` / `INVALID_PLACA` |

Ejemplo de respuesta con respaldo:

```json
{
  "cedula": "1712345678",
  "nombreConductor": "JUAN CARLOS PEREZ GOMEZ",
  "puntos": 26,
  "origen": "CACHE_RESPALDO",
  "esDatoReciente": false,
  "fechaConsulta": "2026-11-17T17:05:11.532Z",
  "fechaConsultaOriginal": "2026-11-12T19:30:00Z",
  "mensaje": "El servicio de la ANT no está disponible en este momento. Mostrando el último registro guardado del 12/11/2026 14:30"
}
```

## Justificación técnica del TTL de caché (7 días para el respaldo de la ANT)

Se guardan **dos claves** por cédula, ambas con el mismo JSON `{ cedula, nombreConductor, puntos, fechaConsulta, origen }`:

| Clave | TTL | Propósito |
|---|---|---|
| `ant:puntos:{cedula}` | **1 hora** (3600 s) | Frescura. Con `ANT_SERVE_FRESH_CACHE=true` evita golpear a la ANT por consultas repetidas |
| `ant:backup:{cedula}` | **7 días** (604800 s) | Respaldo histórico para cuando la ANT no responde |

**Por qué 7 días.** Los puntos de una licencia en Ecuador solo cambian tras citaciones firmes o resoluciones
administrativas: son eventos esporádicos, no cambios minuto a minuto. La ANT, en cambio, sufre caídas frecuentes
(y a veces prolongadas: mantenimientos, fines de semana). Un respaldo de 7 días:

- **garantiza disponibilidad** ante cortes largos del servicio externo, que es el objetivo del patrón;
- **no compromete gravemente la integridad operativa**, porque es improbable que el puntaje real cambie de forma
  relevante en ese lapso y, además, la respuesta nunca oculta que es un dato antiguo (`origen: CACHE_RESPALDO`,
  `esDatoReciente: false`, `fechaConsultaOriginal` y un banner amarillo en la interfaz);
- **es acotado**: transcurridos 7 días sin una consulta exitosa el dato expira y se responde `503` en lugar de mostrar
  información potencialmente obsoleta. Cada consulta exitosa renueva el respaldo (ventana deslizante).

Si el negocio exigiera mayor rigor (p. ej. decisiones de crédito o seguros), basta reducir `ANT_BACKUP_TTL` sin
tocar código.

## Cómo probar el fallback

1. Haz una consulta de puntos que funcione (queda guardada en Redis).
2. Simula la caída de la ANT arrancando el backend con `ANT_BASE_URL=http://localhost:9/x` (puerto cerrado) o con
   `ANT_TIMEOUT_MS=1`.
3. Repite la consulta: el frontend mostrará el **banner amarillo** con la fecha del último registro guardado.
4. Con una cédula que nunca se consultó verás el error `503` con su mensaje amigable.

## Estructura del backend

```
ec.consultas.api
├── controller/   ContribuyenteController · VehiculoController · LicenciaController
├── service/      ContribuyenteService · VehiculoService · LicenciaPuntosService (fallback + TTL)
├── gateway/      SriGateway/SriHttpGateway · AntGateway/AntHttpGateway · HttpFetcher
├── parser/       AntHtmlParser (Jsoup) — único lugar a ajustar si cambia el HTML de la ANT
├── cache/        PuntosStore/RedisPuntosStore (claves ant:puntos / ant:backup)
├── exception/    ApiException · GlobalExceptionHandler (@RestControllerAdvice) · ErrorResponse
├── config/       AppProperties · WebConfig (CORS, Clock)
└── util/         Validators · JsonUtil
```

## Tests

`mvn verify` ejecuta pruebas unitarias de: validadores, parser Jsoup, flujo contribuyente/vehículo (404, 422, mapeo) y
el servicio de licencias (ANT en vivo, fallback con respaldo, 503 sin respaldo, caché fresca).

## Notas y limitaciones conocidas

- **Contratos no oficiales.** El SRI y la ANT no publican una API documentada. El mapeo de campos del SRI es
  tolerante (prueba varios nombres) y el parser de la ANT busca etiquetas «Puntos»/«Nombres» en el HTML. Si alguno
  cambia, solo hay que ajustar `JsonUtil`/los servicios o `AntHtmlParser`.
- **Acceso desde el exterior.** Algunos servicios del Estado ecuatoriano limitan o bloquean tráfico desde fuera de
  Ecuador o desde IPs de centros de datos. Si recibes `502`/`503` constantes, prueba desde una red ecuatoriana o un
  servidor alojado en la región.
- **Certificados.** Si la JVM rechaza el certificado de la ANT (cadena incompleta), importa la CA intermedia en el
  truststore de Java; no se desactiva la verificación TLS.
- El **correo electrónico** del paso 1 solo se valida y se muestra en el cliente; el backend no lo recibe ni lo almacena.
- Las consultas a ANT/SRI se usan con fines informativos; respeta los términos de uso de cada institución.
