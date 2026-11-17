package ec.consultas.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/** Lectura tolerante de JSON: el SRI no publica contrato, así que se prueban varios nombres de campo. */
public final class JsonUtil {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private JsonUtil() {
    }

    /** Primer campo con texto no vacío entre los nombres dados. */
    public static Optional<String> text(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Optional.empty();
        }
        for (String f : fields) {
            JsonNode v = node.get(f);
            if (v != null && !v.isNull() && !v.isContainerNode()) {
                String s = v.asText().trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                    return Optional.of(s);
                }
            }
        }
        return Optional.empty();
    }

    /** Primer campo numérico entre los nombres dados (acepta números en texto). */
    public static Optional<Integer> integer(JsonNode node, String... fields) {
        return text(node, fields).flatMap(s -> {
            try {
                return Optional.of((int) Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    /**
     * Fecha como yyyy-MM-dd. Acepta epoch en milisegundos o texto tipo "2019-05-20 00:00:00.0"
     * / "2019-05-20T00:00:00".
     */
    public static Optional<String> fecha(JsonNode node, String... fields) {
        return text(node, fields).map(s -> {
            if (s.matches("^\\d{11,}$")) {
                return Instant.ofEpochMilli(Long.parseLong(s)).atZone(ECUADOR).toLocalDate().toString();
            }
            if (s.length() >= 10 && s.substring(0, 10).matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                return LocalDate.parse(s.substring(0, 10)).toString();
            }
            return s;
        });
    }
}
