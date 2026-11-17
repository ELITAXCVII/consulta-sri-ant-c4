package ec.consultas.api.controller;

import ec.consultas.api.dto.VehiculoResponse;
import ec.consultas.api.service.VehiculoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehiculos")
public class VehiculoController {

    private final VehiculoService service;

    public VehiculoController(VehiculoService service) {
        this.service = service;
    }

    /** 200 OK | 400 placa inválida | 404 no existe | 502 SRI caído */
    @GetMapping("/{placa}")
    public VehiculoResponse consultar(@PathVariable("placa") String placa) {
        return service.consultar(placa);
    }
}
