package com.ponti.api.controller;

import com.ponti.api.dto.NovaCertificacao;
import com.ponti.api.model.Certificacao;
import com.ponti.api.model.Startup;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.StartupRepository;
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

    public StartupController(StartupRepository repository, CertificacaoRepository certificacaoRepository) {
        this.repository = repository;
        this.certificacaoRepository = certificacaoRepository;
    }

    // Certificações e conquistas da startup (ex.: programas de aceleração), mais recentes primeiro
    @GetMapping("/{id}/certificacoes")
    public List<Certificacao> listarCertificacoes(@PathVariable Long id) {
        garantirQueExiste(id);
        return certificacaoRepository.findByStartupIdOrderByAnoInicioDescIdDesc(id);
    }

    // TODO (PR de segurança): só a própria startup, identificada pelo token, poderá fazer isso
    @PostMapping("/{id}/certificacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Certificacao adicionarCertificacao(@PathVariable Long id, @Valid @RequestBody NovaCertificacao body) {
        garantirQueExiste(id);
        Certificacao certificacao = body.paraEntidade();
        certificacao.setStartupId(id);
        return certificacaoRepository.save(certificacao);
    }

    // TODO (PR de segurança): só a própria startup, identificada pelo token, poderá fazer isso
    @DeleteMapping("/{id}/certificacoes/{certificacaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerCertificacao(@PathVariable Long id, @PathVariable Long certificacaoId) {
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

    // CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Startup criar(@Valid @RequestBody Startup body) {
        body.setId(null);
        return repository.save(body);
    }

    // UPDATE
    @PutMapping("/{id}")
    public Startup atualizar(@PathVariable Long id, @Valid @RequestBody Startup body) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrado(a)");
        }
        body.setId(id);
        return repository.save(body);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrado(a)");
        }
        repository.deleteById(id);
    }
}
