package ec.consultas.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VehiculoResponse(
        String placa,
        String marca,
        String modelo,
        Integer anio,
        Integer cilindraje,
        String clase,
        String servicio,
        String fechaUltimaMatricula,
        String estado) {
}
