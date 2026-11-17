package ec.consultas.api.gateway;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Cliente HTTP mínimo (java.net.http) con timeouts explícitos y User-Agent de navegador. */
@Component
public class HttpFetcher {

    static final String BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/124.0.0.0 Safari/537.36";

    private static final Pattern CHARSET = Pattern.compile("charset=([\\w-]+)", Pattern.CASE_INSENSITIVE);

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    public record Result(int status, byte[] body, String contentType) {

        /** Decodifica con el charset del header; si no hay, UTF-8 y, si queda corrupto, ISO-8859-1. */
        public String bodyAsString() {
            if (body == null || body.length == 0) {
                return "";
            }
            if (contentType != null) {
                Matcher m = CHARSET.matcher(contentType);
                if (m.find()) {
                    try {
                        return new String(body, Charset.forName(m.group(1)));
                    } catch (RuntimeException ignored) {
                        // charset desconocido: se prueba la heurística
                    }
                }
            }
            String utf8 = new String(body, StandardCharsets.UTF_8);
            return utf8.indexOf('�') >= 0 ? new String(body, StandardCharsets.ISO_8859_1) : utf8;
        }
    }

    public Result get(String url, Duration timeout, Map<String, String> headers) throws IOException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", BROWSER_UA)
                .GET();
        headers.forEach(b::header);
        try {
            HttpResponse<byte[]> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new Result(resp.statusCode(), resp.body(),
                    resp.headers().firstValue("Content-Type").orElse(null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Solicitud interrumpida", e);
        }
    }
}
