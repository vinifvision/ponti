package com.ponti.api.controller;

import com.ponti.api.dto.AtividadeResposta;
import com.ponti.api.dto.AvaliacaoEntrega;
import com.ponti.api.dto.NovaAtividade;
import com.ponti.api.dto.NovaEntrega;
import com.ponti.api.dto.NovoComentario;
import com.ponti.api.service.AtividadeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Atividades do acompanhamento e as entregas da startup.
// As regras de quem pode ver e mexer ficam em AtividadeService.
@RestController
public class AtividadeController {

    private final AtividadeService service;

    public AtividadeController(AtividadeService service) {
        this.service = service;
    }

    @GetMapping("/acompanhamentos/{id}/atividades")
    public List<AtividadeResposta> listar(@PathVariable Long id) {
        return service.listar(id);
    }

    // Mentor cria uma atividade (os anexos chegam na parte de arquivos)
    @PostMapping(value = "/acompanhamentos/{id}/atividades", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public AtividadeResposta criar(@PathVariable Long id, @Valid @RequestBody NovaAtividade nova) {
        return service.criar(id, nova);
    }

    // Startup envia ou reenvia
    @PostMapping(value = "/atividades/{id}/entrega", consumes = "application/json")
    public AtividadeResposta entregar(@PathVariable Long id, @Valid @RequestBody NovaEntrega nova) {
        return service.entregar(id, nova);
    }

    // Startup desfaz o último envio (só enquanto o mentor não avaliou)
    @DeleteMapping("/atividades/{id}/entrega")
    public AtividadeResposta desfazer(@PathVariable Long id) {
        return service.desfazerEntrega(id);
    }

    @PostMapping("/atividades/{id}/aprovar")
    public AtividadeResposta aprovar(@PathVariable Long id, @Valid @RequestBody(required = false) AvaliacaoEntrega avaliacao) {
        return service.aprovar(id, avaliacao);
    }

    // Os dois lados comentam numa entrega
    @PostMapping("/atividades/{id}/entregas/{entregaId}/comentarios")
    public AtividadeResposta comentar(@PathVariable Long id, @PathVariable Long entregaId,
                                      @Valid @RequestBody NovoComentario novo) {
        return service.comentar(id, entregaId, novo);
    }

    @PostMapping("/atividades/{id}/devolver")
    public AtividadeResposta devolver(@PathVariable Long id, @Valid @RequestBody(required = false) AvaliacaoEntrega avaliacao) {
        return service.devolver(id, avaliacao);
    }
}
