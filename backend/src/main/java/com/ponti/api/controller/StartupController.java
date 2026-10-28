package com.ponti.api.controller;

import com.ponti.api.dto.NovaCertificacao;
import com.ponti.api.model.Certificacao;
import com.ponti.api.model.Startup;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.seguranca.Acesso;
import com.ponti.api.service.ContaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/startups")
public class StartupController {

    private final StartupRepository repository;
    private final CertificacaoRepository certificacaoRepository;
    private final ContaService contaService;
    private final Acesso acesso;

    public StartupController(StartupRepository repository, CertificacaoRepository certificacaoRepository,
                             ContaService contaService, Acesso acesso) {
        this.repository = repository;
        this.certificacaoRepository = certificacaoRepository;
        this.contaService = contaService;
        this.acesso = acesso;
    }

    // Certificações e conquistas da startup (ex.: programas de aceleração), mais recentes primeiro
    @GetMapping("/{id}/certificacoes")
    public List<Certificacao> listarCertificacoes(@PathVariable Long id) {
        garantirQueExiste(id);
        return certificacaoRepository.findByStartupIdOrderByAnoInicioDescIdDesc(id);
    }

    // Só a própria startup adiciona certificações ao perfil dela
    @PostMapping("/{id}/certificacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Certificacao adicionarCertificacao(@PathVariable Long id, @Valid @RequestBody NovaCertificacao body) {
        acesso.exigirStartup(id);
        garantirQueExiste(id);
        Certificacao certificacao = body.paraEntidade();
        certificacao.setStartupId(id);
        return certificacaoRepository.save(certificacao);
    }

    // Só a própria startup remove certificações do perfil dela
    @DeleteMapping("/{id}/certificacoes/{certificacaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerCertificacao(@PathVariable Long id, @PathVariable Long certificacaoId) {
        acesso.exigirStartup(id);
        Certificacao certificacao = certificacaoRepository.findById(certificacaoId)
                .filter(c -> id.equals(c.getStartupId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificação não encontrada"));
        certificacaoRepository.delete(certificacao);
    }

    private void garantirQueExiste(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrado(a)");
        }
    }

    // READ (todos)
    @GetMapping
    public List<Startup> listar() {
        return repository.findAll();
    }

    // READ (um)
    @GetMapping("/{id}")
    public Startup buscar(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrado(a)"));
    }

    // Startups são criadas pelo cadastro (POST /auth/cadastro/startup), que cria também o login.

    // UPDATE: só a própria startup edita o perfil dela
    @PutMapping("/{id}")
    public Startup atualizar(@PathVariable Long id, @Valid @RequestBody Startup body) {
        acesso.exigirStartup(id);
        garantirQueExiste(id);
        body.setId(id);
        return repository.save(body);
    }

    // DELETE: só a própria startup exclui a conta; leva junto o login e tudo que depende dela
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        acesso.exigirStartup(id);
        contaService.excluirStartup(id);
    }
}
