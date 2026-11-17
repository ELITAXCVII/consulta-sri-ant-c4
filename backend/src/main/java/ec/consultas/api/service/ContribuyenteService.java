package ec.consultas.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import ec.consultas.api.dto.ContribuyenteResponse;
import ec.consultas.api.exception.ApiException;
import ec.consultas.api.gateway.SriGateway;
import ec.consultas.api.util.JsonUtil;
import ec.consultas.api.util.Validators;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ContribuyenteService {

    private final SriGateway sri;

    public ContribuyenteService(SriGateway sri) {
        this.sri = sri;
    }

    public ContribuyenteResponse verificar(String rucRaw) {
        String ruc = Validators.ruc(rucRaw); // 400

        // Paso 1: ¿existe el RUC?
        if (!sri.existeRuc(ruc)) {
            throw noExiste();
        }

        // Paso 2: datos del contribuyente
        JsonNode data = sri.obtenerContribuyente(ruc)
                .map(n -> seleccionar(n, ruc))
                .orElseThrow(ContribuyenteService::noExiste);

        // Paso 3: debe ser persona natural
        if (!esPersonaNatural(data, ruc)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "RUC_NOT_NATURAL_PERSON",
                    "El RUC pertenece a una persona jurídica, se requiere persona natural");
        }

        return new ContribuyenteResponse(
                ruc,
                ruc.substring(0, 10),
                JsonUtil.text(data, "razonSocial", "nombreComercial").orElse(null),
                JsonUtil.text(data, "estadoContribuyenteRuc", "estado").orElse(null),
                JsonUtil.text(data, "actividadContribuyente", "actividadEconomicaPrincipal").orElse(null),
                JsonUtil.text(data, "tipoContribuyente").orElse("PERSONA NATURAL"),
                JsonUtil.text(data, "claseContribuyente").orElse(null),
                JsonUtil.text(data, "regimen").orElse(null),
                JsonUtil.text(data, "obligadoLlevarContabilidad").orElse(null),
                JsonUtil.fecha(data.path("informacionFechasContribuyente"), "fechaInicioActividades").orElse(null));
    }

    /** El SRI devuelve un arreglo; se toma el elemento cuyo numeroRuc coincide (o el primero). */
    static JsonNode seleccionar(JsonNode node, String ruc) {
        if (!node.isArray()) {
            return node;
        }
        for (JsonNode el : node) {
            if (ruc.equals(JsonUtil.text(el, "numeroRuc").orElse(null))) {
                return el;
            }
        }
        return node.get(0);
    }

    /**
     * Persona natural según tipoContribuyente; si el campo no es concluyente se usa la regla
     * del tercer dígito del RUC (menor a 6 = persona natural; 6 = pública; 9 = sociedad privada).
     */
    static boolean esPersonaNatural(JsonNode data, String ruc) {
        String tipo = JsonUtil.text(data, "tipoContribuyente").orElse("").toUpperCase(Locale.ROOT);
        if (tipo.contains("NATURAL")) {
            return true;
        }
        if (tipo.contains("SOCIEDAD") || tipo.contains("JURIDICA") || tipo.contains("JURÍDICA")) {
            return false;
        }
        return ruc.charAt(2) < '6';
    }

    private static ApiException noExiste() {
        return new ApiException(HttpStatus.NOT_FOUND, "RUC_NOT_FOUND", "El RUC ingresado no existe en el SRI");
    }
}
