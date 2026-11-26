package com.ponti.api.controller;

import com.ponti.api.dto.NovaProposta;
import com.ponti.api.dto.ResumoAcompanhamento;
import com.ponti.api.service.AcompanhamentoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Acompanhamento pós-mentoria (aba "Acompanhamentos" do front).
// As regras de quem pode ver e mexer ficam em AcompanhamentoService.
@RestController
@RequestMapping("/acompanhamentos")
public class AcompanhamentoController {

    private final AcompanhamentoService service;

    public AcompanhamentoController(AcompanhamentoService service) {
        this.service = service;
    }

    // Acompanhamentos de quem está logado (startup ou mentor)
    @GetMapping
    public List<ResumoAcompanhamento> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResumoAcompanhamento detalhe(@PathVariable Long id) {
        return service.detalhe(id);
    }

    // O mentor propõe (objetivo + metas iniciais opcionais)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResumoAcompanhamento propor(@Valid @RequestBody NovaProposta nova) {
        return service.propor(nova);
    }

    // A startup responde à proposta
    @PostMapping("/{id}/aceitar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aceitar(@PathVariable Long id) {
        service.aceitar(id);
    }

    @PostMapping("/{id}/recusar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recusar(@PathVariable Long id) {
        service.recusar(id);
    }
}
