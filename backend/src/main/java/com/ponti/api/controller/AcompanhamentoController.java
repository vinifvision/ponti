package com.ponti.api.controller;

import com.ponti.api.dto.AtualizacaoMeta;
import com.ponti.api.dto.MetaResposta;
import com.ponti.api.dto.NovaProposta;
import com.ponti.api.dto.PainelConsolidado;
import com.ponti.api.dto.ResumoAcompanhamento;
import com.ponti.api.service.AcompanhamentoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Acompanhamento pós-mentoria (aba "Acompanhamentos" do front).
// As regras de quem pode ver e mexer ficam em AcompanhamentoService.
@RestController
public class AcompanhamentoController {

    private final AcompanhamentoService service;

    public AcompanhamentoController(AcompanhamentoService service) {
        this.service = service;
    }

    // Acompanhamentos de quem está logado (startup ou mentor)
    @GetMapping("/acompanhamentos")
    public List<ResumoAcompanhamento> listar() {
        return service.listar();
    }

    // Painel do topo da lista (soma dos acompanhamentos ativos)
    @GetMapping("/acompanhamentos/painel")
    public PainelConsolidado painel() {
        return service.painel();
    }

    @GetMapping("/acompanhamentos/{id}")
    public ResumoAcompanhamento detalhe(@PathVariable Long id) {
        return service.detalhe(id);
    }

    // O mentor propõe (objetivo + metas iniciais opcionais)
    @PostMapping("/acompanhamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumoAcompanhamento propor(@Valid @RequestBody NovaProposta nova) {
        return service.propor(nova);
    }

    // A startup responde à proposta
    @PostMapping("/acompanhamentos/{id}/aceitar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aceitar(@PathVariable Long id) {
        service.aceitar(id);
    }

    @PostMapping("/acompanhamentos/{id}/recusar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recusar(@PathVariable Long id) {
        service.recusar(id);
    }

    // O mentor encerra (depois disso, somente leitura)
    @PostMapping("/acompanhamentos/{id}/encerrar")
    public ResumoAcompanhamento encerrar(@PathVariable Long id) {
        return service.encerrar(id);
    }

    // Metas do acompanhamento (os dois lados veem; só o mentor marca como cumprida)
    @GetMapping("/acompanhamentos/{id}/metas")
    public List<MetaResposta> metas(@PathVariable Long id) {
        return service.metas(id);
    }

    @PatchMapping("/metas/{id}")
    public MetaResposta atualizarMeta(@PathVariable Long id, @Valid @RequestBody AtualizacaoMeta atualizacao) {
        return service.atualizarMeta(id, atualizacao);
    }
}
