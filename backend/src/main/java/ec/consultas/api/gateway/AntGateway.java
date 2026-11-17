package ec.consultas.api.gateway;

import ec.consultas.api.exception.AntUnavailableException;

public interface AntGateway {

    /** Resultado ya parseado de la ANT. */
    record AntPuntos(String nombreConductor, int puntos) {
    }

    /**
     * Consulta la ANT en vivo.
     *
     * @param placa placa opcional; cadena vacía si no se envía
     * @throws AntUnavailableException timeout, HTTP no-200, error de conexión o HTML sin datos
     */
    AntPuntos consultar(String cedula, String placa);
}
