package ec.consultas.api.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/** Acceso al SRI. Lanza ApiException 502 (SRI_UNAVAILABLE) si el SRI no responde correctamente. */
public interface SriGateway {

    /** Paso 1: existePorNumeroRuc. */
    boolean existeRuc(String ruc);

    /** Paso 2: obtenerPorNumerosRuc. Vacío si el SRI no devuelve datos. */
    Optional<JsonNode> obtenerContribuyente(String ruc);

    /** Vacío si la placa no existe. */
    Optional<JsonNode> obtenerVehiculo(String placa);
}
