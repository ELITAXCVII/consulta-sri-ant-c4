package ec.consultas.api.controller;

import ec.consultas.api.dto.PuntosLicenciaResponse;
import ec.consultas.api.service.LicenciaPuntosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/licencias")
public class LicenciaController {

    private final LicenciaPuntosService service;

    public LicenciaController(LicenciaPuntosService service) {
        this.service = service;
    }

    /**
     * 200 OK (en vivo o desde caché de respaldo) | 400 cédula/placa inválida
     * | 503 ANT caída y sin dato guardado.
     */
    @GetMapping("/{cedula}/puntos")
    public PuntosLicenciaResponse puntos(
            @PathVariable("cedula") String cedula,
            @RequestParam(name = "placa", required = false) String placa) {
        return service.consultar(cedula, placa);
    }
}
