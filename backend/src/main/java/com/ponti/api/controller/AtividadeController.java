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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    // Mentor cria uma atividade. Sem arquivos chega como JSON; com arquivos, como multipart/form-data
    // (campos titulo, descricao, prazo e arquivos), do jeito que o acompanhamento.service.ts envia.
    @PostMapping(value = "/acompanhamentos/{id}/atividades", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AtividadeResposta criar(@PathVariable Long id, @Valid @RequestBody NovaAtividade nova) {
        return service.criar(id, nova, List.of());
    }

    @PostMapping(value = "/acompanhamentos/{id}/atividades", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AtividadeResposta criarComArquivos(@PathVariable Long id, @Valid @ModelAttribute NovaAtividade nova,
                                              @RequestPart(name = "arquivos", required = false) List<MultipartFile> arquivos) {
        return service.criar(id, nova, arquivos);
    }

    // Startup envia ou reenvia (texto, link e/ou arquivos)
    @PostMapping(value = "/atividades/{id}/entrega", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AtividadeResposta entregar(@PathVariable Long id, @Valid @RequestBody NovaEntrega nova) {
        return service.entregar(id, nova, List.of());
    }

    @PostMapping(value = "/atividades/{id}/entrega", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AtividadeResposta entregarComArquivos(@PathVariable Long id, @Valid @ModelAttribute NovaEntrega nova,
                                                 @RequestPart(name = "arquivos", required = false) List<MultipartFile> arquivos) {
        return service.entregar(id, nova, arquivos);
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
