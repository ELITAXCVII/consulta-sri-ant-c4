package ec.consultas.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Puntos de licencia. {@code origen} puede ser ANT_EN_VIVO, CACHE_FRESCA o CACHE_RESPALDO.
 * Cuando es CACHE_RESPALDO, {@code esDatoReciente} es false y {@code mensaje} trae la advertencia.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PuntosLicenciaResponse(
        String cedula,
        String nombreConductor,
        Integer puntos,
        String origen,
        boolean esDatoReciente,
        Instant fechaConsulta,
        Instant fechaConsultaOriginal,
        String mensaje) {
}
