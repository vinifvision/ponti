package com.ponti.api.controller;

import com.ponti.api.dto.NovaCertificacao;
import com.ponti.api.model.Certificacao;
import com.ponti.api.model.Mentor;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.MentorRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/mentores")
public class MentorController {

    private final MentorRepository repository;
    private final CertificacaoRepository certificacaoRepository;

    public MentorController(MentorRepository repository, CertificacaoRepository certificacaoRepository) {
        this.repository = repository;
        this.certificacaoRepository = certificacaoRepository;
    }

    // Certificações e experiências do mentor, mais recentes primeiro (seção "Experiência" do perfil)
    @GetMapping("/{id}/certificacoes")
    public List<Certificacao> listarCertificacoes(@PathVariable Long id) {
        garantirQueExiste(id);
        return certificacaoRepository.findByMentorIdOrderByAnoInicioDescIdDesc(id);
    }

    // Adiciona uma certificação ao perfil do mentor
    // TODO (PR de segurança): só o próprio mentor, identificado pelo token, poderá fazer isso
    @PostMapping("/{id}/certificacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Certificacao adicionarCertificacao(@PathVariable Long id, @Valid @RequestBody NovaCertificacao body) {
        garantirQueExiste(id);
        Certificacao certificacao = body.paraEntidade();
        certificacao.setMentorId(id);
        return certificacaoRepository.save(certificacao);
    }

    // Remove uma certificação do perfil do mentor
    // TODO (PR de segurança): só o próprio mentor, identificado pelo token, poderá fazer isso
    @DeleteMapping("/{id}/certificacoes/{certificacaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerCertificacao(@PathVariable Long id, @PathVariable Long certificacaoId) {
        Certificacao certificacao = certificacaoRepository.findById(certificacaoId)
                .filter(c -> id.equals(c.getMentorId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificação não encontrada"));
        certificacaoRepository.delete(certificacao);
    }

    private void garantirQueExiste(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado(a)");
        }
    }

    // READ (todos)
    @GetMapping
    public List<Mentor> listar() {
        return repository.findAll();
    }

    // READ (um)
    @GetMapping("/{id}")
    public Mentor buscar(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado(a)"));
    }

    // CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mentor criar(@Valid @RequestBody Mentor body) {
        body.setId(null);
        return repository.save(body);
    }

    // UPDATE
    @PutMapping("/{id}")
    public Mentor atualizar(@PathVariable Long id, @Valid @RequestBody Mentor body) {
        Mentor atual = repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado(a)"));
        body.setId(id);
        // O WhatsApp nunca volta nas respostas, então quem edita o perfil não o reenvia:
        // se não vier no corpo, mantém o que já estava gravado.
        if (body.getWhatsapp() == null) {
            body.setWhatsapp(atual.getWhatsapp());
        }
        return repository.save(body);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado(a)");
        }
        repository.deleteById(id);
    }
}
