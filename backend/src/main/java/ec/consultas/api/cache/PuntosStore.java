package ec.consultas.api.cache;

import java.util.Optional;

/** Almacén de puntos de licencia. La caché es "best effort": nunca debe romper una consulta. */
public interface PuntosStore {

    /** Clave de frescura {@code ant:puntos:{cedula}} (TTL corto). */
    Optional<PuntosCacheEntry> getFresh(String cedula);

    /** Clave de respaldo {@code ant:backup:{cedula}} (TTL de 7 días). */
    Optional<PuntosCacheEntry> getBackup(String cedula);

    /** Guarda el dato bueno en ambas claves. */
    void save(PuntosCacheEntry entry);
}
