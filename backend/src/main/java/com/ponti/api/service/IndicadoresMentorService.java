package com.ponti.api.service;

import static java.util.stream.Collectors.groupingBy;

import com.ponti.api.config.RankingPropriedades;
import com.ponti.api.config.RelogioConfig;
import com.ponti.api.model.Agendamento;
import com.ponti.api.model.Disponibilidade;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.MentorIndicadores;
import com.ponti.api.model.StatusAgendamento;
import com.ponti.api.model.StatusPagamento;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.AvaliacaoRepository;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.DisponibilidadeRepository;
import com.ponti.api.repository.MentorIndicadoresRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.UsuarioRepository;
import com.ponti.api.service.RankingMentores.Indicadores;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Indicadores dos mentores para a ordenação dos recomendados (ver RankingMentores).
//
// A parte cara (agenda, respostas, confiabilidade, avaliações, mentorias realizadas) fica pré-calculada
// na tabela MentorIndicadores e é recalculada:
//   - a cada 15 minutos, para todos os mentores (cobre a passagem do tempo);
//   - na hora, só do mentor afetado: os services chamam recalcular(mentorId) quando um agendamento é
//     criado, pago, confirmado, recusado ou cancelado, quando a agenda muda e quando entra uma avaliação.
// A busca (coletar) só lê a tabela e junta o que depende da startup ou é barato (match, perfil completo,
// último acesso, data de cadastro, "recusou esta startup").
@Service
public class IndicadoresMentorService {

    private static final Logger log = LoggerFactory.getLogger(IndicadoresMentorService.class);

    // Confiabilidade: pedidos recebidos nos últimos 90 dias
    static final int JANELA_CONFIABILIDADE_DIAS = 90;
    // Tempo de resposta: mediana dos últimos 10 pedidos respondidos
    // (o mesmo 10 do findTop10... no AgendamentoRepository)
    static final int ULTIMOS_PEDIDOS_RESPOSTA = 10;
    // "Tem horários esta semana"
    static final int JANELA_SEMANA_DIAS = 7;

    private final MentorRepository mentorRepository;
    private final MentorIndicadoresRepository indicadoresRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final CertificacaoRepository certificacaoRepository;
    private final RankingPropriedades parametros;
    private final Clock relogio;

    public IndicadoresMentorService(MentorRepository mentorRepository,
                                    MentorIndicadoresRepository indicadoresRepository,
                                    UsuarioRepository usuarioRepository,
                                    DisponibilidadeRepository disponibilidadeRepository,
                                    AgendamentoRepository agendamentoRepository,
                                    AvaliacaoRepository avaliacaoRepository,
                                    CertificacaoRepository certificacaoRepository,
                                    RankingPropriedades parametros,
                                    Clock relogio) {
        this.mentorRepository = mentorRepository;
        this.indicadoresRepository = indicadoresRepository;
        this.usuarioRepository = usuarioRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.certificacaoRepository = certificacaoRepository;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    // ================================================================ recálculo

    // Recalcula a linha de um mentor (recálculo por evento). Se o mentor não existe mais, apaga a linha.
    // Roda na mesma transação de quem chamou: a consulta já enxerga a mudança que disparou o recálculo.
    @Transactional
    public void recalcular(Long mentorId) {
        if (mentorId == null) return;
        Mentor mentor = mentorRepository.findById(mentorId).orElse(null);
        if (mentor == null) {
            indicadoresRepository.deleteById(mentorId);
            return;
        }
        indicadoresRepository.save(calcularUm(mentor));
    }

    // Recalcula todos os mentores: ao subir a aplicação (depois dos dados de demonstração) e a cada
    // ponti.ranking.recalculo-ms (15 minutos). Usa uma consulta agrupada por assunto, não uma por mentor.
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(fixedDelayString = "${ponti.ranking.recalculo-ms:900000}",
               initialDelayString = "${ponti.ranking.recalculo-ms:900000}")
    @Transactional
    public void recalcularTodos() {
        List<Mentor> mentores = mentorRepository.findAll();
        Momento m = momento();

        Map<Long, List<Disponibilidade>> faixas = disponibilidadeRepository.findAll().stream()
                .collect(groupingBy(Disponibilidade::getMentorId));
        Map<Long, List<Agendamento>> ocupando = agendamentoRepository
                .ocupandoHorarioDeTodos(AgendaService.OCUPAM_HORARIO, m.agoraLocal(), m.fimJanela()).stream()
                .collect(groupingBy(Agendamento::getMentorId));
        // groupingBy mantém a ordem da consulta: os mais recentes primeiro
        Map<Long, List<Agendamento>> respondidos = agendamentoRepository
                .findByRespondidoEmNotNullOrderByRespondidoEmDesc().stream()
                .collect(groupingBy(Agendamento::getMentorId));
        Map<Long, List<Agendamento>> recebidos = agendamentoRepository
                .findByCriadoEmGreaterThanEqual(m.inicioConfiabilidade()).stream()
                .collect(groupingBy(Agendamento::getMentorId));
        Map<Long, Long> realizadas = new HashMap<>();
        for (Object[] linha : agendamentoRepository.realizadasPorMentor(m.agoraLocal())) {
            realizadas.put((Long) linha[0], ((Number) linha[1]).longValue());
        }
        Map<Long, double[]> notas = new HashMap<>();
        for (Object[] linha : avaliacaoRepository.somaPorMentor()) {
            notas.put((Long) linha[0], new double[] {((Number) linha[1]).doubleValue(), ((Number) linha[2]).longValue()});
        }

        List<MentorIndicadores> linhas = mentores.stream().map(mentor -> {
            Long id = mentor.getId();
            double[] nota = notas.getOrDefault(id, new double[] {0, 0});
            return calcular(mentor, new Dados(
                    faixas.getOrDefault(id, List.of()),
                    ocupando.getOrDefault(id, List.of()),
                    respondidos.getOrDefault(id, List.of()),
                    recebidos.getOrDefault(id, List.of()),
                    nota[0], (long) nota[1],
                    realizadas.getOrDefault(id, 0L)), m);
        }).toList();
        indicadoresRepository.saveAll(linhas);

        // Linhas de mentores que não existem mais (ex.: conta excluída fora do ContaService)
        Set<Long> existentes = mentores.stream().map(Mentor::getId).collect(Collectors.toSet());
        indicadoresRepository.findAll().stream()
                .filter(linha -> !existentes.contains(linha.getMentorId()))
                .forEach(indicadoresRepository::delete);

        log.debug("Indicadores do ranking recalculados para {} mentores", linhas.size());
    }

    // Calcula a linha de um mentor só, com consultas filtradas por ele (não grava)
    private MentorIndicadores calcularUm(Mentor mentor) {
        Long id = mentor.getId();
        Momento m = momento();
        Object[] nota = avaliacaoRepository.somaDoMentor(id).get(0);
        return calcular(mentor, new Dados(
                disponibilidadeRepository.findByMentorId(id),
                agendamentoRepository.ocupandoHorario(id, AgendaService.OCUPAM_HORARIO, m.agoraLocal(), m.fimJanela()),
                agendamentoRepository.findTop10ByMentorIdAndRespondidoEmNotNullOrderByRespondidoEmDesc(id),
                agendamentoRepository.findByMentorIdAndCriadoEmGreaterThanEqual(id, m.inicioConfiabilidade()),
                ((Number) nota[0]).doubleValue(), ((Number) nota[1]).longValue(),
                agendamentoRepository.countByMentorIdAndStatusAndFimBefore(id, StatusAgendamento.CONFIRMADO, m.agoraLocal())), m);
    }

    // ================================================================ busca

    // Os indicadores de todos os mentores, vistos pela startup que busca, e a média geral da plataforma
    public record Coleta(List<Indicadores> indicadores, double mediaGeral) {
    }

    @Transactional(readOnly = true)
    public Coleta coletar(Long startupId, Map<Long, Integer> compatibilidades) {
        Instant agora = relogio.instant();
        Map<Long, MentorIndicadores> tabela = indicadoresRepository.findAll().stream()
                .collect(Collectors.toMap(MentorIndicadores::getMentorId, Function.identity()));
        Set<Long> recusaram = recusaramEstaStartup(startupId, agora);
        Set<Long> comCertificacao = new HashSet<>(certificacaoRepository.mentoresComCertificacao());
        Map<Long, Instant> ultimoAcesso = ultimoAcessoPorMentor();
        Object[] geral = avaliacaoRepository.somaGeral().get(0);

        List<Indicadores> indicadores = mentorRepository.findAll().stream().map(mentor -> {
            Long id = mentor.getId();
            // Mentor ainda sem linha (cadastrado direto no banco, antes do próximo recálculo): calcula na hora
            MentorIndicadores linha = tabela.containsKey(id) ? tabela.get(id) : calcularUm(mentor);
            return new Indicadores(
                    id,
                    compatibilidades.getOrDefault(id, 0),
                    linha.getOcupacao(),
                    linha.isTemHorario7d(),
                    linha.isTemHorario14d(),
                    linha.getTempoRespostaMedianoHoras(),
                    linha.getPedidosRespondidos(),
                    linha.getTaxaConfiabilidade(),
                    linha.getSomaNotas(),
                    linha.getTotalAvaliacoes(),
                    itensDoPerfil(mentor, comCertificacao.contains(id)),
                    ultimoAcesso.get(id),
                    mentor.getCriadoEm(),
                    linha.getMentoriasRealizadas(),
                    recusaram.contains(id));
        }).toList();

        double mediaGeral = RankingMentores.mediaGeral(((Number) geral[0]).doubleValue(), ((Number) geral[1]).longValue());
        return new Coleta(indicadores, mediaGeral);
    }

    // ================================================================ cálculo de uma linha

    // O "agora" e as janelas, lidos uma vez do relógio para o recálculo inteiro usar o mesmo instante
    record Momento(Instant agora, LocalDateTime agoraLocal, LocalDateTime fimJanela, LocalDateTime fimSemana,
                   Instant inicioConfiabilidade) {
    }

    private Momento momento() {
        Instant agora = relogio.instant();
        LocalDateTime agoraLocal = LocalDateTime.now(relogio);
        return new Momento(agora, agoraLocal,
                agoraLocal.plusDays(parametros.janelaAgendaDias()),
                agoraLocal.plusDays(JANELA_SEMANA_DIAS),
                agora.minus(Duration.ofDays(JANELA_CONFIABILIDADE_DIAS)));
    }

    // Tudo o que o cálculo precisa de um mentor, já buscado do banco
    record Dados(List<Disponibilidade> faixas, List<Agendamento> ocupando, List<Agendamento> respondidos,
                 List<Agendamento> recebidos, double somaNotas, long totalAvaliacoes, long mentoriasRealizadas) {
    }

    static MentorIndicadores calcular(Mentor mentor, Dados d, Momento m) {
        Agenda agenda = agenda(mentor, d.faixas(), d.ocupando(), m.agoraLocal(), m.fimJanela(), m.fimSemana());
        List<Double> tempos = d.respondidos().stream()
                .limit(ULTIMOS_PEDIDOS_RESPOSTA)
                .map(IndicadoresMentorService::horasAteResponder)
                .toList();

        MentorIndicadores linha = new MentorIndicadores();
        linha.setMentorId(mentor.getId());
        linha.setOcupacao(agenda.ocupacao());
        linha.setTemHorario7d(agenda.temHorario7d());
        linha.setTemHorario14d(agenda.temHorario14d());
        linha.setTempoRespostaMedianoHoras(mediana(tempos));
        linha.setPedidosRespondidos(tempos.size());
        linha.setTaxaConfiabilidade(taxaConfiabilidade(d.recebidos()));
        linha.setSomaNotas(d.somaNotas());
        linha.setTotalAvaliacoes(d.totalAvaliacoes());
        linha.setMentoriasRealizadas(d.mentoriasRealizadas());
        linha.setAtualizadoEm(m.agora());
        return linha;
    }

    // ---------------------------------------------------------------- agenda

    record Agenda(Double ocupacao, boolean temHorario7d, boolean temHorario14d) {
    }

    // Olha a janela (agora até agora + janelaAgendaDias): quais blocos o mentor oferece, quais estão livres
    // e quanto do tempo oferecido já está reservado. Usa as mesmas regras de horários do AgendaService.
    static Agenda agenda(Mentor mentor, List<Disponibilidade> faixas, List<Agendamento> ocupando,
                         LocalDateTime agora, LocalDateTime fimJanela, LocalDateTime fimSemana) {
        int duracao = AgendaService.duracao(mentor);
        List<LocalDateTime> oferecidos = AgendaService
                .blocosOferecidos(faixas, duracao, agora.toLocalDate(), fimJanela.toLocalDate()).stream()
                .filter(inicio -> inicio.isAfter(agora) && !inicio.plusMinutes(duracao).isAfter(fimJanela))
                .toList();
        List<LocalDateTime> livres = oferecidos.stream()
                .filter(inicio -> !AgendaService.conflita(inicio, inicio.plusMinutes(duracao), ocupando))
                .toList();

        long minutosOferecidos = (long) oferecidos.size() * duracao;
        long minutosReservados = ocupando.stream()
                .filter(IndicadoresMentorService::reservado)
                .mapToLong(a -> Math.max(0, Duration.between(maior(a.getInicio(), agora), menor(a.getFim(), fimJanela)).toMinutes()))
                .sum();
        Double ocupacao = minutosOferecidos == 0 ? null : Math.min(1.0, (double) minutosReservados / minutosOferecidos);

        return new Agenda(ocupacao, livres.stream().anyMatch(inicio -> inicio.isBefore(fimSemana)), !livres.isEmpty());
    }

    // Reservado = confirmado, ou pendente com o pagamento já aprovado (Pix aguardando ainda não conta)
    static boolean reservado(Agendamento a) {
        return a.getStatus() == StatusAgendamento.CONFIRMADO
                || (a.getStatus() == StatusAgendamento.PENDENTE
                    && (a.getStatusPagamento() == StatusPagamento.PAGO || a.getStatusPagamento() == StatusPagamento.GRATUITA));
    }

    // ---------------------------------------------------------------- respostas

    // Do momento em que o pedido chegou ao mentor até a resposta. Pedido pago por Pix só chega ao mentor
    // depois do pagamento, então conta a partir do pagamento quando ele é posterior à criação.
    static double horasAteResponder(Agendamento a) {
        Instant chegou = a.getCriadoEm();
        if (a.getPagoEm() != null) {
            Instant pago = a.getPagoEm().atZone(RelogioConfig.FUSO).toInstant();
            if (pago.isAfter(chegou)) chegou = pago;
        }
        return Math.max(0, Duration.between(chegou, a.getRespondidoEm()).toMinutes() / 60.0);
    }

    // 1 − (recusas + cancelamentos feitos pelo mentor) ÷ pedidos recebidos. Sem pedidos: null (neutro).
    // Pix que nunca foi pago (aguardando ou expirado) não chegou ao mentor e não conta.
    static Double taxaConfiabilidade(List<Agendamento> pedidos) {
        List<Agendamento> recebidos = pedidos.stream()
                .filter(a -> a.getStatusPagamento() != StatusPagamento.AGUARDANDO
                        && a.getStatusPagamento() != StatusPagamento.EXPIRADO)
                .toList();
        if (recebidos.isEmpty()) return null;
        long falhas = recebidos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.RECUSADO
                        || (a.getStatus() == StatusAgendamento.CANCELADO && a.getCanceladoPor() == TipoUsuario.MENTOR))
                .count();
        return 1.0 - (double) falhas / recebidos.size();
    }

    static Double mediana(List<Double> valores) {
        if (valores.isEmpty()) return null;
        List<Double> ordenados = valores.stream().sorted().toList();
        int meio = ordenados.size() / 2;
        return ordenados.size() % 2 == 1 ? ordenados.get(meio) : (ordenados.get(meio - 1) + ordenados.get(meio)) / 2;
    }

    // ================================================================ partes calculadas na busca

    // Mentores que recusaram um pedido desta startup dentro da janela. A data da recusa é respondidoEm;
    // em registros antigos (sem respondidoEm) usa a data do pedido: se o pedido é recente, a recusa também é.
    private Set<Long> recusaramEstaStartup(Long startupId, Instant agora) {
        Instant limite = agora.minus(Duration.ofDays(parametros.recusaDias()));
        return agendamentoRepository.findByStartupIdAndStatus(startupId, StatusAgendamento.RECUSADO).stream()
                .filter(a -> !(a.getRespondidoEm() != null ? a.getRespondidoEm() : a.getCriadoEm()).isBefore(limite))
                .map(Agendamento::getMentorId)
                .collect(Collectors.toSet());
    }

    // Foto, vídeo, bio, LinkedIn e ao menos uma certificação
    static int itensDoPerfil(Mentor m, boolean temCertificacao) {
        int itens = 0;
        if (preenchido(m.getFotoUrl())) itens++;
        if (preenchido(m.getVideoUrl())) itens++;
        if (preenchido(m.getBio())) itens++;
        if (preenchido(m.getLinkedinUrl())) itens++;
        if (temCertificacao) itens++;
        return itens;
    }

    private Map<Long, Instant> ultimoAcessoPorMentor() {
        return usuarioRepository.findByMentorIdNotNull().stream()
                .filter(u -> u.getUltimoAcessoEm() != null)
                .collect(Collectors.toMap(Usuario::getMentorId, Usuario::getUltimoAcessoEm,
                        (a, b) -> a.isAfter(b) ? a : b));
    }

    private static boolean preenchido(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static LocalDateTime maior(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDateTime menor(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
