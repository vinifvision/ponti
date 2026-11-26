package com.ponti.api.service;

import com.ponti.api.dto.NovaProposta;
import com.ponti.api.dto.ParceiroAcompanhamento;
import com.ponti.api.dto.ResumoAcompanhamento;
import com.ponti.api.model.Acompanhamento;
import com.ponti.api.model.Atividade;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Meta;
import com.ponti.api.model.StatusAcompanhamento;
import com.ponti.api.model.StatusAtividade;
import com.ponti.api.model.Startup;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.repository.AcompanhamentoRepository;
import com.ponti.api.repository.AtividadeRepository;
import com.ponti.api.repository.EntregaRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.MetaRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.seguranca.Acesso;
import com.ponti.api.seguranca.UsuarioLogado;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Regras do módulo de acompanhamentos. Resumo das permissões:
//  - só o mentor e a startup do acompanhamento enxergam ele; para os outros, "não encontrado" (404),
//    assim ninguém descobre nem que o acompanhamento existe;
//  - só o mentor propõe; só a startup aceita ou recusa.
@Service
public class AcompanhamentoService {

    public static final String NAO_ENCONTRADO = "Acompanhamento não encontrado.";

    // Situações que aparecem na lista (a proposta recusada sai, como no front)
    private static final Set<StatusAcompanhamento> VISIVEIS =
            EnumSet.of(StatusAcompanhamento.PROPOSTO, StatusAcompanhamento.ATIVO, StatusAcompanhamento.ENCERRADO);
    private static final Set<StatusAcompanhamento> EM_ANDAMENTO =
            EnumSet.of(StatusAcompanhamento.PROPOSTO, StatusAcompanhamento.ATIVO);

    private final AcompanhamentoRepository repository;
    private final MetaRepository metaRepository;
    private final AtividadeRepository atividadeRepository;
    private final EntregaRepository entregaRepository;
    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;
    private final Acesso acesso;
    private final Clock relogio;

    public AcompanhamentoService(AcompanhamentoRepository repository,
                                 MetaRepository metaRepository,
                                 AtividadeRepository atividadeRepository,
                                 EntregaRepository entregaRepository,
                                 StartupRepository startupRepository,
                                 MentorRepository mentorRepository,
                                 Acesso acesso,
                                 Clock relogio) {
        this.repository = repository;
        this.metaRepository = metaRepository;
        this.atividadeRepository = atividadeRepository;
        this.entregaRepository = entregaRepository;
        this.startupRepository = startupRepository;
        this.mentorRepository = mentorRepository;
        this.acesso = acesso;
        this.relogio = relogio;
    }

    // ----- consultas -----

    @Transactional(readOnly = true)
    public List<ResumoAcompanhamento> listar() {
        UsuarioLogado logado = acesso.atual();
        List<Acompanhamento> lista = logado.tipo() == TipoUsuario.MENTOR
                ? repository.findByMentorIdAndStatusInOrderByCriadoEmDesc(logado.mentorId(), VISIVEIS)
                : repository.findByStartupIdAndStatusInOrderByCriadoEmDesc(logado.startupId(), VISIVEIS);
        return lista.stream().map(a -> resumir(a, logado)).toList();
    }

    @Transactional(readOnly = true)
    public ResumoAcompanhamento detalhe(Long id) {
        UsuarioLogado logado = acesso.atual();
        return resumir(doParticipante(id, logado), logado);
    }

    // ----- proposta -----

    @Transactional
    public ResumoAcompanhamento propor(NovaProposta nova) {
        UsuarioLogado logado = acesso.atual();
        if (logado.tipo() != TipoUsuario.MENTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Acesso.SEM_PERMISSAO);
        }
        Long mentorId = logado.mentorId();
        if (!startupRepository.existsById(nova.startupId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrada.");
        }
        if (!repository.teveMentoriaRealizada(mentorId, nova.startupId(), LocalDateTime.now(relogio))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você só pode propor um acompanhamento a uma startup com quem já teve uma mentoria realizada.");
        }
        if (repository.existsByMentorIdAndStartupIdAndStatusIn(mentorId, nova.startupId(), EM_ANDAMENTO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma proposta ou um acompanhamento em andamento com esta startup.");
        }

        Acompanhamento a = new Acompanhamento();
        a.setMentorId(mentorId);
        a.setStartupId(nova.startupId());
        a.setObjetivo(nova.objetivo().trim());
        a = repository.save(a);

        List<String> titulos = nova.metas() == null ? List.of()
                : nova.metas().stream().filter(t -> t != null && !t.isBlank()).map(String::trim).toList();
        for (int i = 0; i < titulos.size(); i++) {
            Meta meta = new Meta();
            meta.setAcompanhamentoId(a.getId());
            meta.setTitulo(titulos.get(i));
            meta.setOrdem(i);
            metaRepository.save(meta);
        }
        return resumir(a, logado);
    }

    @Transactional
    public void aceitar(Long id) {
        Acompanhamento a = propostaDaStartupLogada(id);
        a.setStatus(StatusAcompanhamento.ATIVO);
        a.setDataInicio(LocalDate.now(relogio));
    }

    @Transactional
    public void recusar(Long id) {
        Acompanhamento a = propostaDaStartupLogada(id);
        a.setStatus(StatusAcompanhamento.RECUSADO);
    }

    // ----- exclusão de conta (LGPD) -----

    // Apaga os acompanhamentos da pessoa e tudo o que está dentro deles (metas, atividades e entregas).
    // Chamado por ContaService, dentro da mesma transação da exclusão da conta.
    @Transactional
    public void apagarDaStartup(Long startupId) {
        repository.findByStartupId(startupId).forEach(this::apagar);
    }

    @Transactional
    public void apagarDoMentor(Long mentorId) {
        repository.findByMentorId(mentorId).forEach(this::apagar);
    }

    private void apagar(Acompanhamento a) {
        for (Atividade atividade : atividadeRepository.findByAcompanhamentoIdOrderByPrazoAscIdAsc(a.getId())) {
            entregaRepository.deleteByAtividadeId(atividade.getId());
        }
        atividadeRepository.deleteByAcompanhamentoId(a.getId());
        metaRepository.deleteByAcompanhamentoId(a.getId());
        repository.delete(a);
    }

    // ----- apoio -----

    // Busca o acompanhamento e confere se quem está logado é um dos dois lados (senão: 404)
    Acompanhamento doParticipante(Long id, UsuarioLogado logado) {
        Acompanhamento a = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NAO_ENCONTRADO));
        if (!a.ehParticipante(logado.startupId(), logado.mentorId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, NAO_ENCONTRADO);
        }
        return a;
    }

    private Acompanhamento propostaDaStartupLogada(Long id) {
        UsuarioLogado logado = acesso.atual();
        Acompanhamento a = doParticipante(id, logado);
        if (logado.tipo() != TipoUsuario.STARTUP) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Acesso.SEM_PERMISSAO);
        }
        if (a.getStatus() != StatusAcompanhamento.PROPOSTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta proposta não está mais pendente.");
        }
        return a;
    }

    // Monta o card. Os números são contados agora, a partir das metas e atividades guardadas no banco.
    ResumoAcompanhamento resumir(Acompanhamento a, UsuarioLogado logado) {
        List<Meta> metas = metaRepository.findByAcompanhamentoIdOrderByOrdemAsc(a.getId());
        List<Atividade> atividades = atividadeRepository.findByAcompanhamentoIdOrderByPrazoAscIdAsc(a.getId());
        LocalDateTime agora = LocalDateTime.now(relogio);
        boolean proposta = a.getStatus() == StatusAcompanhamento.PROPOSTO;
        boolean ehMentor = logado.tipo() == TipoUsuario.MENTOR;

        int cumpridas = (int) metas.stream().filter(Meta::isCumprida).count();
        int aprovadas = (int) atividades.stream().filter(x -> x.getStatus() == StatusAtividade.APROVADA).count();
        int atrasadas = (int) atividades.stream().filter(x -> x.estaAtrasada(agora)).count();
        // Mentor: entregas esperando a revisão dele. Startup: atividades que ela ainda precisa entregar.
        int pendencias = (int) (ehMentor
                ? atividades.stream().filter(x -> x.getStatus() == StatusAtividade.ENTREGUE).count()
                : atividades.stream().filter(Atividade::aEntregar).count());
        // Próximo prazo que ainda não venceu, entre as atividades a entregar (já vêm ordenadas por prazo)
        LocalDate proximoPrazo = atividades.stream()
                .filter(x -> x.aEntregar() && !x.getPrazo().isBefore(agora))
                .map(x -> x.getPrazo().toLocalDate())
                .findFirst().orElse(null);

        return new ResumoAcompanhamento(
                a.getId(), a.getStatus(), a.getObjetivo(), a.getDataInicio(), a.getDataFim(),
                parceiro(a, logado),
                aprovadas, atividades.size(),
                proposta ? 0 : cumpridas,
                proposta ? 0 : metas.size(),
                proposta ? metas.size() : null,
                proximoPrazo, atrasadas, pendencias);
    }

    private ParceiroAcompanhamento parceiro(Acompanhamento a, UsuarioLogado logado) {
        if (logado.tipo() == TipoUsuario.MENTOR) {
            Startup s = startupRepository.findById(a.getStartupId()).orElse(null);
            if (s == null) return new ParceiroAcompanhamento(a.getStartupId(), "Startup", null, null);
            return new ParceiroAcompanhamento(s.getId(), s.getNome(), s.getFotoUrl(), subtituloStartup(s));
        }
        Mentor m = mentorRepository.findById(a.getMentorId()).orElse(null);
        if (m == null) return new ParceiroAcompanhamento(a.getMentorId(), "Mentor", null, null);
        return new ParceiroAcompanhamento(m.getId(), m.getNome(), m.getFotoUrl(), m.getAreasAtuacao());
    }

    // Ex.: "Nível 4, Pet" (nível de maturidade e segmento, quando existirem)
    private static String subtituloStartup(Startup s) {
        String nivel = s.getEstagioMaturidade() != null ? "Nível " + s.getEstagioMaturidade() : null;
        String segmento = s.getSegmento() != null && !s.getSegmento().isBlank() ? s.getSegmento() : null;
        if (nivel != null && segmento != null) return nivel + ", " + segmento;
        return nivel != null ? nivel : segmento;
    }
}
