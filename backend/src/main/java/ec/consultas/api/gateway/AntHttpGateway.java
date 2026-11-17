package ec.consultas.api.gateway;

import ec.consultas.api.config.AppProperties;
import ec.consultas.api.exception.AntUnavailableException;
import ec.consultas.api.parser.AntHtmlParser;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AntHttpGateway implements AntGateway {

    private static final Map<String, String> HTML_HEADERS = Map.of(
            "Accept", "text/html,application/xhtml+xml,*/*;q=0.8",
            "Accept-Language", "es-EC,es;q=0.9");

    private final HttpFetcher http;
    private final String baseUrl;
    private final Duration timeout;

    public AntHttpGateway(HttpFetcher http, AppProperties props) {
        this.http = http;
        this.baseUrl = props.ant().baseUrl();
        this.timeout = Duration.ofMillis(props.ant().timeoutMs());
    }

    @Override
    public AntPuntos consultar(String cedula, String placa) {
        String url = baseUrl
                + "?ps_tipo_identificacion=CED"
                + "&ps_identificacion=" + URLEncoder.encode(cedula, StandardCharsets.UTF_8)
                + "&ps_placa=" + URLEncoder.encode(placa == null ? "" : placa, StandardCharsets.UTF_8);

        HttpFetcher.Result r;
        try {
            r = http.get(url, timeout, HTML_HEADERS);
        } catch (IOException e) { // incluye HttpTimeoutException y errores de conexión/TLS
            throw new AntUnavailableException("No se pudo conectar con la ANT: " + e, e);
        }
        if (r.status() != 200) {
            throw new AntUnavailableException("La ANT respondió HTTP " + r.status());
        }
        String html = r.bodyAsString();
        if (html.isBlank()) {
            throw new AntUnavailableException("La ANT devolvió una respuesta vacía");
        }
        return AntHtmlParser.parse(html)
                .orElseThrow(() -> new AntUnavailableException("El HTML de la ANT no contiene puntos de licencia"));
    }
}
