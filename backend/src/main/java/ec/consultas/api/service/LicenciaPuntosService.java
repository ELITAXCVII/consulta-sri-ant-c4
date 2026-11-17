package ec.consultas.api.service;

import ec.consultas.api.cache.PuntosCacheEntry;
import ec.consultas.api.cache.PuntosStore;
import ec.consultas.api.config.AppProperties;
import ec.consultas.api.dto.PuntosLicenciaResponse;
import ec.consultas.api.exception.AntUnavailableException;
import ec.consultas.api.exception.ApiException;
import ec.consultas.api.gateway.AntGateway;
import ec.consultas.api.gateway.AntGateway.AntPuntos;
import ec.consultas.api.util.Validators;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Consulta de puntos con patrón Stale-While-Error.
 *
 * <p><b>Por qué 7 días de respaldo (604800 s):</b> los puntos de una licencia en Ecuador solo cambian
 * tras citaciones firmes o resoluciones administrativas, es decir, de forma esporádica y no minuto a
 * minuto. La ANT, en cambio, se cae con frecuencia. Un respaldo de 7 días cubre cortes largos
 * (fines de semana, mantenimientos) sin servir información que pueda estar gravemente desactualizada.
 * Cada consulta exitosa renueva el respaldo, y la respuesta siempre informa que es un dato antiguo
 * (origen = CACHE_RESPALDO, esDatoReciente = false, fechaConsultaOriginal).
 *
 * <p>Se usan dos claves: {@code ant:puntos:{cedula}} (frescura, TTL 1 h) y
 * {@code ant:backup:{cedula}} (respaldo, TTL 7 d).
 */
@Service
public class LicenciaPuntosService {

    static final String ORIGEN_EN_VIVO = "ANT_EN_VIVO";
    static final String ORIGEN_CACHE_FRESCA = "CACHE_FRESCA";
    static final String ORIGEN_RESPALDO = "CACHE_RESPALDO";

    private static final Logger log = LoggerFactory.getLogger(LicenciaPuntosService.class);
    private static final DateTimeFormatter FECHA_ES =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Guayaquil"));

    private final AntGateway ant;
    private final PuntosStore store;
    private final Clock clock;
    private final boolean serveFreshCache;

    public LicenciaPuntosService(AntGateway ant, PuntosStore store, Clock clock, AppProperties props) {
        this.ant = ant;
        this.store = store;
        this.clock = clock;
        this.serveFreshCache = props.ant().serveFreshCache();
    }

    public PuntosLicenciaResponse consultar(String cedulaRaw, String placaRaw) {
        String cedula = Validators.cedula(cedulaRaw);       // 400
        String placa = Validators.placaOpcional(placaRaw);  // 400

        if (serveFreshCache) {
            Optional<PuntosCacheEntry> fresca = store.getFresh(cedula);
            if (fresca.isPresent()) {
                return respuesta(fresca.get(), ORIGEN_CACHE_FRESCA, true, null);
            }
        }

        try {
            AntPuntos vivo = ant.consultar(cedula, placa);
            Instant ahora = clock.instant();
            PuntosCacheEntry entry = new PuntosCacheEntry(cedula, vivo.nombreConductor(), vivo.puntos(), ahora, ORIGEN_EN_VIVO);
            store.save(entry); // best effort: nunca rompe la respuesta
            return respuesta(entry, ORIGEN_EN_VIVO, true, null);
        } catch (AntUnavailableException e) {
            log.warn("ANT no disponible para cédula terminada en {}: {}", cedula.substring(6), e.getMessage());
            return store.getBackup(cedula)
                    .map(this::desdeRespaldo)
                    .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ANT_UNAVAILABLE",
                            "El servicio de la ANT no está disponible en este momento y no hay un registro guardado "
                                    + "para esta cédula. Intenta nuevamente en unos minutos."));
        }
    }

    private PuntosLicenciaResponse desdeRespaldo(PuntosCacheEntry e) {
        String aviso = "El servicio de la ANT no está disponible en este momento. "
                + "Mostrando el último registro guardado del " + FECHA_ES.format(e.fechaConsulta());
        return respuesta(e, ORIGEN_RESPALDO, false, aviso);
    }

    private PuntosLicenciaResponse respuesta(PuntosCacheEntry e, String origen, boolean reciente, String mensaje) {
        boolean enVivo = ORIGEN_EN_VIVO.equals(origen);
        return new PuntosLicenciaResponse(
                e.cedula(),
                e.nombreConductor(),
                e.puntos(),
                origen,
                reciente,
                enVivo ? e.fechaConsulta() : clock.instant(),
                e.fechaConsulta(),
                mensaje);
    }
}
