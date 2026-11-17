package ec.consultas.api.controller;

import ec.consultas.api.dto.ContribuyenteResponse;
import ec.consultas.api.service.ContribuyenteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contribuyentes")
public class ContribuyenteController {

    private final ContribuyenteService service;

    public ContribuyenteController(ContribuyenteService service) {
        this.service = service;
    }

    /** 200 OK | 400 RUC inválido | 404 no existe | 422 persona jurídica | 502 SRI caído */
    @GetMapping("/{ruc}")
    public ContribuyenteResponse verificar(@PathVariable("ruc") String ruc) {
        return service.verificar(ruc);
    }
}
