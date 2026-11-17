package ec.consultas.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Datos limpios del contribuyente (persona natural) devueltos por el SRI. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContribuyenteResponse(
        String ruc,
        String cedula,
        String razonSocial,
        String estado,
        String actividadEconomica,
        String tipoContribuyente,
        String claseContribuyente,
        String regimen,
        String obligadoLlevarContabilidad,
        String fechaInicioActividades) {
}
