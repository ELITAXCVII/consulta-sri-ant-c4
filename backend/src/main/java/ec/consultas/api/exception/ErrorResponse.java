package ec.consultas.api.exception;

import java.time.Instant;

/** Formato único de error: { "code": "...", "message": "...", "timestamp": "..." }. */
public record ErrorResponse(String code, String message, Instant timestamp) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Instant.now());
    }
}
