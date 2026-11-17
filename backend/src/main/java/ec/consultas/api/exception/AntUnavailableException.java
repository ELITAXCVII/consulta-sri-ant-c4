package ec.consultas.api.exception;

/**
 * La ANT no respondió correctamente (timeout, HTTP 5xx, error de conexión o HTML sin datos).
 * Es una señal interna: el servicio la captura para activar el fallback de caché.
 */
public class AntUnavailableException extends RuntimeException {

    public AntUnavailableException(String message) {
        super(message);
    }

    public AntUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
