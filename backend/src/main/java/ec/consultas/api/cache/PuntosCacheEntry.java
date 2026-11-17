package ec.consultas.api.cache;

import java.time.Instant;

/** Objeto JSON guardado en Redis: { cedula, nombreConductor, puntos, fechaConsulta, origen }. */
public record PuntosCacheEntry(
        String cedula,
        String nombreConductor,
        Integer puntos,
        Instant fechaConsulta,
        String origen) {
}
