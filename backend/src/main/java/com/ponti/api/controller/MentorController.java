package com.ponti.api.controller;

import com.ponti.api.dto.AtualizacaoMentor;
import com.ponti.api.dto.AvaliacaoPublica;
import com.ponti.api.dto.ContatoPrivado;
import com.ponti.api.dto.MentorRecomendado;
import com.ponti.api.dto.MentorResposta;
import com.ponti.api.dto.NovaCertificacao;
import com.ponti.api.dto.Pagina;
import com.ponti.api.model.Certificacao;
import com.ponti.api.model.Mentor;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.seguranca.Acesso;
import com.ponti.api.seguranca.UsuarioLogado;
import com.ponti.api.service.AvaliacaoService;
import com.ponti.api.service.BuscaMentores;
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

    // Contato privado do próprio mentor (tela "Editar perfil"). Ninguém mais acessa.
    @GetMapping("/{id}/contato")
    public ContatoPrivado contato(@PathVariable Long id) {
        acesso.exigirMentor(id);
        Mentor m = mentorService.buscarEntidade(id);
        return new ContatoPrivado(m.getWhatsapp(), m.isCompartilharWhatsapp());
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

    // Busca da startup logada: mentores na ordem de recomendação (ver RankingMentores), com filtros e
    // paginação (ver BuscaMentores). Só startups usam esta lista; o mentor logado recebe 403.
    @GetMapping("/recomendados")
    public Pagina<MentorRecomendado> recomendados(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String experiencia,
            @RequestParam(required = false) String faixaPreco,
            @RequestParam(required = false) Boolean comHorarioEm7Dias,
            @RequestParam(required = false) String ordenacao,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        UsuarioLogado logado = acesso.atual();
        if (logado.startupId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Acesso.SEM_PERMISSAO);
        }
        BuscaMentores.Filtro filtro = BuscaMentores.Filtro.de(q, area, experiencia, faixaPreco, comHorarioEm7Dias,
                ordenacao, pagina, tamanho, BuscaMentores.Ordenacao.RECOMENDADOS);
        return matchService.buscarParaStartup(logado.startupId(), filtro);
    }

    // Busca geral (quem não é startup): mesmos filtros e paginação, em ordem alfabética. Cada mentor já
    // vem com a reputação (nota média e mentorias realizadas).
    @GetMapping
    public Pagina<MentorResposta> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String experiencia,
            @RequestParam(required = false) String faixaPreco,
            @RequestParam(required = false) Boolean comHorarioEm7Dias,
            @RequestParam(required = false) String ordenacao,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        BuscaMentores.Filtro filtro = BuscaMentores.Filtro.de(q, area, experiencia, faixaPreco, comHorarioEm7Dias,
                ordenacao, pagina, tamanho, BuscaMentores.Ordenacao.NOME);
        return mentorService.pesquisar(filtro);
    }

    // Todas as áreas atendidas pelos mentores, para as opções do filtro "Área de atuação".
    // Com a paginação, a tela não tem mais todos os mentores para montar essa lista sozinha.
    @GetMapping("/areas")
    public List<String> areas() {
        return mentorService.areas();
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
        Mentor atual = repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado(a)"));
        body.setId(id);
        // O WhatsApp e a autorização não vêm no JSON (@JsonIgnore); sem isto o PUT os apagaria
        body.setWhatsapp(atual.getWhatsapp());
        body.setCompartilharWhatsapp(atual.isCompartilharWhatsapp());
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
