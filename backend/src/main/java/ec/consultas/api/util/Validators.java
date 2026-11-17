package ec.consultas.api.util;

import ec.consultas.api.exception.ApiException;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/** Validaciones de formato de entrada. Todas devuelven 400 con un código estable y mensaje claro. */
public final class Validators {

    private static final Pattern RUC = Pattern.compile("^\\d{10}001$");
    private static final Pattern CEDULA_O_RUC = Pattern.compile("^\\d{10}(\\d{3})?$");
    /** Placas de auto: 3 letras + 3 o 4 dígitos (ABC123, ABC1234, ABC0123). */
    private static final Pattern PLACA_AUTO = Pattern.compile("^[A-Z]{3}\\d{3,4}$");
    /** Placas de moto: 2 letras + 3 dígitos + 1 letra (AB123C). */
    private static final Pattern PLACA_MOTO = Pattern.compile("^[A-Z]{2}\\d{3}[A-Z]$");

    private Validators() {
    }

    /** RUC de persona natural: 13 dígitos numéricos terminados en 001. */
    public static String ruc(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (!RUC.matcher(v).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RUC",
                    "El RUC debe tener 13 dígitos numéricos y terminar en 001 (por ejemplo, 1712345678001).");
        }
        return v;
    }

    /** Cédula de 10 dígitos. Si llega un RUC de 13 dígitos se toman los 10 primeros. */
    public static String cedula(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (!CEDULA_O_RUC.matcher(v).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CEDULA",
                    "La cédula debe tener 10 dígitos numéricos (o un RUC de 13 dígitos).");
        }
        return v.substring(0, 10);
    }

    /** Placa normalizada en mayúsculas y sin guiones; ej. "abc-1234" -> "ABC1234". */
    public static String placa(String raw) {
        String v = raw == null ? "" : raw.trim().replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (!PLACA_AUTO.matcher(v).matches() && !PLACA_MOTO.matcher(v).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PLACA",
                    "La placa no tiene un formato válido. Usa 3 letras y 3 o 4 números (por ejemplo, ABC1234).");
        }
        return v;
    }

    /** Placa opcional (query param): vacía o ausente devuelve cadena vacía. */
    public static String placaOpcional(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return placa(raw);
    }
}
