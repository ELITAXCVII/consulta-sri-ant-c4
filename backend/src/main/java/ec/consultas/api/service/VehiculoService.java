package ec.consultas.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import ec.consultas.api.dto.VehiculoResponse;
import ec.consultas.api.exception.ApiException;
import ec.consultas.api.gateway.SriGateway;
import ec.consultas.api.util.JsonUtil;
import ec.consultas.api.util.Validators;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class VehiculoService {

    private final SriGateway sri;

    public VehiculoService(SriGateway sri) {
        this.sri = sri;
    }

    public VehiculoResponse consultar(String placaRaw) {
        String placa = Validators.placa(placaRaw); // 400

        JsonNode raw = sri.obtenerVehiculo(placa).orElseThrow(() -> noExiste(placa));
        JsonNode v = raw.isArray() ? raw.get(0) : raw;

        String marca = JsonUtil.text(v, "descripcionMarca", "marca").orElse(null);
        String modelo = JsonUtil.text(v, "descripcionModelo", "modelo").orElse(null);
        if (marca == null && modelo == null) {
            throw noExiste(placa); // el SRI respondió algo sin datos de vehículo
        }

        return new VehiculoResponse(
                JsonUtil.text(v, "numeroPlaca", "placa", "numeroPlacaCampvCpn").orElse(placa),
                marca,
                modelo,
                JsonUtil.integer(v, "anioAuto", "anio", "anioFabricacion").orElse(null),
                JsonUtil.integer(v, "cilindraje").orElse(null),
                JsonUtil.text(v, "descripcionClase", "clase").orElse(null),
                JsonUtil.text(v, "descripcionServicio", "servicio").orElse(null),
                JsonUtil.fecha(v, "fechaUltimaMatricula", "fechaMatricula", "fechaUltimaMatriculacion").orElse(null),
                JsonUtil.text(v, "estadoAuto", "estado", "descripcionEstado").orElse(null));
    }

    private static ApiException noExiste(String placa) {
        return new ApiException(HttpStatus.NOT_FOUND, "VEHICLE_NOT_FOUND",
                "No se encontró un vehículo con la placa " + placa + " en el SRI");
    }
}
