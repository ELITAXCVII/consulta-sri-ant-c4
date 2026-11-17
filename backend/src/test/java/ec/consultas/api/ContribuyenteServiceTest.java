package ec.consultas.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.consultas.api.dto.ContribuyenteResponse;
import ec.consultas.api.dto.VehiculoResponse;
import ec.consultas.api.exception.ApiException;
import ec.consultas.api.gateway.SriGateway;
import ec.consultas.api.service.ContribuyenteService;
import ec.consultas.api.service.VehiculoService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ContribuyenteServiceTest {

    private static final ObjectMapper M = new ObjectMapper();
    private static final String RUC = "1712345678001";

    private static JsonNode json(String s) {
        try {
            return M.readTree(s);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static SriGateway sri(boolean existe, String contribuyenteJson, String vehiculoJson) {
        return new SriGateway() {
            public boolean existeRuc(String ruc) { return existe; }
            public Optional<JsonNode> obtenerContribuyente(String ruc) {
                return contribuyenteJson == null ? Optional.empty() : Optional.of(json(contribuyenteJson));
            }
            public Optional<JsonNode> obtenerVehiculo(String placa) {
                return vehiculoJson == null ? Optional.empty() : Optional.of(json(vehiculoJson));
            }
        };
    }

    @Test
    void rucInexistenteDevuelve404() {
        assertThatThrownBy(() -> new ContribuyenteService(sri(false, null, null)).verificar(RUC))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getMessage()).isEqualTo("El RUC ingresado no existe en el SRI");
                });
    }

    @Test
    void sociedadDevuelve422() {
        String sociedad = "[{\"numeroRuc\":\"" + RUC + "\",\"razonSocial\":\"ACME S.A.\",\"tipoContribuyente\":\"SOCIEDAD\"}]";
        assertThatThrownBy(() -> new ContribuyenteService(sri(true, sociedad, null)).verificar(RUC))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(e.getCode()).isEqualTo("RUC_NOT_NATURAL_PERSON");
                });
    }

    @Test
    void personaNaturalDevuelveDatosLimpios() {
        String natural = "[{\"numeroRuc\":\"" + RUC + "\",\"razonSocial\":\"PEREZ JUAN\","
                + "\"estadoContribuyenteRuc\":\"ACTIVO\",\"actividadContribuyente\":\"SERVICIOS DE SOFTWARE\","
                + "\"tipoContribuyente\":\"PERSONA NATURAL\","
                + "\"informacionFechasContribuyente\":{\"fechaInicioActividades\":\"2018-03-05 00:00:00.0\"}}]";
        ContribuyenteResponse r = new ContribuyenteService(sri(true, natural, null)).verificar(RUC);
        assertThat(r.ruc()).isEqualTo(RUC);
        assertThat(r.cedula()).isEqualTo("1712345678");
        assertThat(r.razonSocial()).isEqualTo("PEREZ JUAN");
        assertThat(r.estado()).isEqualTo("ACTIVO");
        assertThat(r.fechaInicioActividades()).isEqualTo("2018-03-05");
    }

    @Test
    void vehiculoMapeaCampos() {
        String v = "{\"numeroPlaca\":\"ABC1234\",\"descripcionMarca\":\"CHEVROLET\",\"descripcionModelo\":\"AVEO\","
                + "\"anioAuto\":2015,\"cilindraje\":1600,\"descripcionClase\":\"AUTOMOVIL\","
                + "\"fechaUltimaMatricula\":\"2024-02-10 00:00:00.0\",\"estadoAuto\":\"CIRCULACION\"}";
        VehiculoResponse r = new VehiculoService(sri(true, null, v)).consultar("abc-1234");
        assertThat(r.placa()).isEqualTo("ABC1234");
        assertThat(r.marca()).isEqualTo("CHEVROLET");
        assertThat(r.anio()).isEqualTo(2015);
        assertThat(r.cilindraje()).isEqualTo(1600);
        assertThat(r.fechaUltimaMatricula()).isEqualTo("2024-02-10");
    }

    @Test
    void vehiculoInexistenteDevuelve404() {
        assertThatThrownBy(() -> new VehiculoService(sri(true, null, null)).consultar("ABC1234"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
