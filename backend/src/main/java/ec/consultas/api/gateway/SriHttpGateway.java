package ec.consultas.api.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.consultas.api.config.AppProperties;
import ec.consultas.api.exception.ApiException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SriHttpGateway implements SriGateway {

    private static final String CATASTRO = "/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente";
    private static final String VEHICULO =
            "/sri-matriculacion-vehicular-recaudacion-servicio-internet/rest/BaseVehiculo"
                    + "/obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn";
    private static final Map<String, String> JSON_HEADERS = Map.of("Accept", "application/json, text/plain, */*");

    private final HttpFetcher http;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final Duration timeout;

    public SriHttpGateway(HttpFetcher http, ObjectMapper mapper, AppProperties props) {
        this.http = http;
        this.mapper = mapper;
        this.baseUrl = props.sri().baseUrl();
        this.timeout = Duration.ofMillis(props.sri().timeoutMs());
    }

    @Override
    public boolean existeRuc(String ruc) {
        HttpFetcher.Result r = call(baseUrl + CATASTRO + "/existePorNumeroRuc?numeroRuc=" + enc(ruc));
        if (r.status() == 404) {
            return false;
        }
        requireOk(r);
        return "true".equalsIgnoreCase(r.bodyAsString().trim());
    }

    @Override
    public Optional<JsonNode> obtenerContribuyente(String ruc) {
        // La URL del SRI lleva "?&ruc=" tal cual está publicada.
        HttpFetcher.Result r = call(baseUrl + CATASTRO + "/obtenerPorNumerosRuc?&ruc=" + enc(ruc));
        return parseJsonOrEmpty(r);
    }

    @Override
    public Optional<JsonNode> obtenerVehiculo(String placa) {
        HttpFetcher.Result r = call(baseUrl + VEHICULO + "?numeroPlacaCampvCpn=" + enc(placa));
        return parseJsonOrEmpty(r);
    }

    private Optional<JsonNode> parseJsonOrEmpty(HttpFetcher.Result r) {
        if (r.status() == 404) {
            return Optional.empty();
        }
        requireOk(r);
        String body = r.bodyAsString().trim();
        if (body.isEmpty() || "null".equals(body)) {
            return Optional.empty();
        }
        try {
            JsonNode node = mapper.readTree(body);
            if (node == null || node.isNull() || node.isMissingNode()
                    || (node.isContainerNode() && node.isEmpty())) {
                return Optional.empty();
            }
            return Optional.of(node);
        } catch (JsonProcessingException e) {
            throw unavailable(e);
        }
    }

    private HttpFetcher.Result call(String url) {
        try {
            return http.get(url, timeout, JSON_HEADERS);
        } catch (IOException e) {
            throw unavailable(e);
        }
    }

    private void requireOk(HttpFetcher.Result r) {
        if (r.status() < 200 || r.status() >= 300) {
            throw unavailable(new IOException("El SRI respondió HTTP " + r.status()));
        }
    }

    private static ApiException unavailable(Throwable cause) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "SRI_UNAVAILABLE",
                "El servicio del SRI no está disponible en este momento. Intenta nuevamente en unos minutos.", cause);
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
