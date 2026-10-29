package com.ponti.api.controller;

import com.ponti.api.dto.AtualizacaoMentor;
import com.ponti.api.dto.AvaliacaoPublica;
import com.ponti.api.dto.MentorRecomendado;
import com.ponti.api.dto.MentorResposta;
import com.ponti.api.dto.NovaCertificacao;
import com.ponti.api.model.Certificacao;
import com.ponti.api.model.Mentor;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.seguranca.Acesso;
import com.ponti.api.seguranca.UsuarioLogado;
import com.ponti.api.service.AvaliacaoService;
import com.ponti.api.service.ContaService;
import com.ponti.api.service.MatchService;
import com.ponti.api.service.MentorService;
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
    private final ContaService contaService;
    private final Acesso acesso;
    private final MentorService mentorService;
    private final AvaliacaoService avaliacaoService;
    private final MatchService matchService;

    public MentorController(MentorRepository repository, CertificacaoRepository certificacaoRepository,
                            ContaService contaService, Acesso acesso,
                            MentorService mentorService, AvaliacaoService avaliacaoService,
                            MatchService matchService) {
        this.repository = repository;
        this.certificacaoRepository = certificacaoRepository;
        this.contaService = contaService;
        this.acesso = acesso;
        this.mentorService = mentorService;
        this.avaliacaoService = avaliacaoService;
        this.matchService = matchService;
    }

    // Avaliações que o mentor recebeu, mais recentes primeiro
    @GetMapping("/{id}/avaliacoes")
    public List<AvaliacaoPublica> avaliacoes(@PathVariable Long id) {
        return avaliacaoService.doMentor(id);
    }

    // Certificações e experiências do mentor, mais recentes primeiro (seção "Experiência" do perfil)
    @GetMapping("/{id}/certificacoes")
    public List<Certificacao> listarCertificacoes(@PathVariable Long id) {
        garantirQueExiste(id);
        return certificacaoRepository.findByMentorIdOrderByAnoInicioDescIdDesc(id);
    }

    // Adiciona uma certificação ao perfil do mentor
    // Só o próprio mentor adiciona certificações ao perfil dele
    @PostMapping("/{id}/certificacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Certificacao adicionarCertificacao(@PathVariable Long id, @Valid @RequestBody NovaCertificacao body) {
        acesso.exigirMentor(id);
        garantirQueExiste(id);
        Certificacao certificacao = body.paraEntidade();
        certificacao.setMentorId(id);
        return certificacaoRepository.save(certificacao);
    }

    // Remove uma certificação do perfil do mentor
    // Só o próprio mentor remove certificações do perfil dele
    @DeleteMapping("/{id}/certificacoes/{certificacaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerCertificacao(@PathVariable Long id, @PathVariable Long certificacaoId) {
        acesso.exigirMentor(id);
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

    // Mentores ordenados pela compatibilidade com a startup logada (temas que ela precisa e setor).
    // Só startups usam esta lista; o mentor logado recebe 403.
    @GetMapping("/recomendados")
    public List<MentorRecomendado> recomendados() {
        UsuarioLogado logado = acesso.atual();
        if (logado.startupId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Acesso.SEM_PERMISSAO);
        }
        return matchService.mentoresParaStartup(logado.startupId());
    }

    // READ (todos), já com a reputação de cada mentor (nota média e mentorias realizadas)
    @GetMapping
    public List<MentorResposta> listar() {
        return mentorService.listar();
    }

    // READ (um)
    @GetMapping("/{id}")
    public MentorResposta buscar(@PathVariable Long id) {
        return mentorService.buscar(id);
    }

    // Mentores são criados pelo cadastro (POST /auth/cadastro/mentor), que cria também o login.

    // UPDATE: só o próprio mentor edita o perfil dele
    @PutMapping("/{id}")
    public Mentor atualizar(@PathVariable Long id, @Valid @RequestBody Mentor body) {
        acesso.exigirMentor(id);
        garantirQueExiste(id);
        body.setId(id);
        return repository.save(body);
    }

    // UPDATE parcial: só os campos enviados mudam (usado pela tela "Meu perfil")
    @PatchMapping("/{id}")
    public MentorResposta atualizarParcial(@PathVariable Long id, @Valid @RequestBody AtualizacaoMentor dados) {
        acesso.exigirMentor(id);
        return mentorService.atualizar(id, dados);
    }

    // DELETE: só o próprio mentor exclui a conta; leva junto o login, a agenda e tudo que depende dele
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        acesso.exigirMentor(id);
        contaService.excluirMentor(id);
    }
}
