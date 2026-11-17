package ec.consultas.api.parser;

import ec.consultas.api.gateway.AntGateway.AntPuntos;
import java.text.Normalizer;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Extrae puntos y nombre del HTML de clp_grid_citaciones.jsp.
 *
 * <p>La ANT no publica un contrato: el parser es deliberadamente tolerante (busca etiquetas
 * "Puntos" / "Nombres" y toma el valor del mismo elemento tras ':' o del elemento hermano siguiente,
 * con un respaldo por expresión regular sobre el texto plano). Si el HTML de la ANT cambia, este
 * es el ÚNICO archivo que hay que ajustar.
 */
public final class AntHtmlParser {

    /** La licencia en Ecuador arranca con 30 puntos; un valor fuera de 0..30 no es un puntaje. */
    static final int MAX_PUNTOS = 30;

    private static final Pattern ETIQUETA_PUNTOS =
            Pattern.compile("^(total de |saldo de |puntos? en la licencia|puntos? de licencia)?\\s*(puntos?|puntaje)\\b");
    private static final Pattern ETIQUETA_NOMBRE = Pattern.compile(
            "^(apellidos y nombres|nombres y apellidos|nombres?|nombre del (conductor|titular)|conductor|titular|propietario)\\b");
    private static final Pattern DIGITOS = Pattern.compile("(\\d{1,3})");
    private static final Pattern PUNTOS_EN_TEXTO = Pattern.compile("puntos[^0-9]{0,60}?(\\d{1,3})");

    private AntHtmlParser() {
    }

    public static Optional<AntPuntos> parse(String html) {
        if (html == null || html.isBlank()) {
            return Optional.empty();
        }
        Document doc = Jsoup.parse(html);
        if (doc.text().isBlank()) {
            return Optional.empty();
        }

        Optional<Integer> puntos = valorDeEtiqueta(doc, ETIQUETA_PUNTOS)
                .flatMap(AntHtmlParser::primerPuntaje)
                .or(() -> puntosEnTextoPlano(doc.text()));
        if (puntos.isEmpty()) {
            return Optional.empty();
        }

        String nombre = valorDeEtiqueta(doc, ETIQUETA_NOMBRE)
                .map(String::trim)
                .filter(n -> !n.isEmpty() && !n.matches("^[\\d\\s.,-]+$") && n.length() <= 120)
                .orElse(null);
        return Optional.of(new AntPuntos(nombre, puntos.get()));
    }

    private static Optional<String> valorDeEtiqueta(Document doc, Pattern etiqueta) {
        for (Element el : doc.select("td, th, label, span, b, strong, dt, p, div")) {
            if (el.children().size() > 3) {
                continue; // contenedor grande, no una etiqueta
            }
            String texto = el.text().trim();
            String normalizado = normalizar(texto);
            if (normalizado.isEmpty() || normalizado.length() > 80 || !etiqueta.matcher(normalizado).find()) {
                continue;
            }
            // Si un hijo ya es la etiqueta, se prefiere el elemento más específico (evita arrastrar celdas vecinas)
            boolean hijoEsEtiqueta = el.children().stream()
                    .anyMatch(c -> etiqueta.matcher(normalizar(c.text())).find());
            if (hijoEsEtiqueta) {
                continue;
            }
            int dosPuntos = texto.indexOf(':');
            if (dosPuntos >= 0 && dosPuntos < texto.length() - 1) {
                String valor = texto.substring(dosPuntos + 1).trim();
                if (!valor.isEmpty()) {
                    return Optional.of(valor);
                }
            }
            // <b>Etiqueta:</b> valor   (texto suelto justo después)
            Node siguiente = el.nextSibling();
            if (siguiente instanceof TextNode t && !t.isBlank()) {
                return Optional.of(t.text().trim());
            }
            Element hermano = el.nextElementSibling();
            if (hermano != null && !hermano.text().isBlank()) {
                return Optional.of(hermano.text().trim());
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> primerPuntaje(String valor) {
        Matcher m = DIGITOS.matcher(valor);
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n >= 0 && n <= MAX_PUNTOS) {
                return Optional.of(n);
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> puntosEnTextoPlano(String texto) {
        Matcher m = PUNTOS_EN_TEXTO.matcher(normalizar(texto));
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n >= 0 && n <= MAX_PUNTOS) {
                return Optional.of(n);
            }
        }
        return Optional.empty();
    }

    /** Minúsculas, sin tildes y con espacios colapsados. */
    static String normalizar(String s) {
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinTildes.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
