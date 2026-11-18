package com.ponti.api.service;

import static java.util.stream.Collectors.groupingBy;

import com.ponti.api.config.RankingPropriedades;
import com.ponti.api.config.RelogioConfig;
import com.ponti.api.model.Agendamento;
import com.ponti.api.model.Disponibilidade;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.StatusAgendamento;
import com.ponti.api.model.StatusPagamento;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.AvaliacaoRepository;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.DisponibilidadeRepository;
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
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Junta, do banco, o que o RankingMentores precisa saber de cada mentor (agenda, respostas, avaliações,
// perfil, último acesso). Calcula tudo na hora da busca, com uma consulta agrupada por assunto
// (não uma por mentor). Quando a lista de mentores crescer, a etapa 4 do documento troca isto por uma
// tabela pré-calculada (MentorIndicadores), sem mudar o RankingMentores.
@Service
public class IndicadoresMentorService {

    // Confiabilidade: pedidos recebidos nos últimos 90 dias
    static final int JANELA_CONFIABILIDADE_DIAS = 90;
    // Tempo de resposta: mediana dos últimos 10 pedidos respondidos
    static final int ULTIMOS_PEDIDOS_RESPOSTA = 10;
    // "Tem horários esta semana"
    static final int JANELA_SEMANA_DIAS = 7;

    private final MentorRepository mentorRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final CertificacaoRepository certificacaoRepository;
    private final RankingPropriedades parametros;
    private final Clock relogio;

    public IndicadoresMentorService(MentorRepository mentorRepository,
                                    UsuarioRepository usuarioRepository,
                                    DisponibilidadeRepository disponibilidadeRepository,
                                    AgendamentoRepository agendamentoRepository,
                                    AvaliacaoRepository avaliacaoRepository,
                                    CertificacaoRepository certificacaoRepository,
                                    RankingPropriedades parametros,
                                    Clock relogio) {
        this.mentorRepository = mentorRepository;
        this.usuarioRepository = usuarioRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.certificacaoRepository = certificacaoRepository;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    // Os indicadores de todos os mentores, vistos pela startup que busca, e a média geral da plataforma
    public record Coleta(List<Indicadores> indicadores, double mediaGeral) {
    }

    @Transactional(readOnly = true)
    public Coleta coletar(Long startupId, Map<Long, Integer> compatibilidades) {
        Instant agora = relogio.instant();
        LocalDateTime agoraLocal = LocalDateTime.now(relogio);
        LocalDateTime fimJanela = agoraLocal.plusDays(parametros.janelaAgendaDias());
        LocalDateTime fimSemana = agoraLocal.plusDays(JANELA_SEMANA_DIAS);

        Map<Long, List<Disponibilidade>> faixas = disponibilidadeRepository.findAll().stream()
                .collect(groupingBy(Disponibilidade::getMentorId));
        Map<Long, List<Agendamento>> ocupando = agendamentoRepository
                .ocupandoHorarioDeTodos(AgendaService.OCUPAM_HORARIO, agoraLocal, fimJanela).stream()
                .collect(groupingBy(Agendamento::getMentorId));
        // groupingBy mantém a ordem da consulta: os mais recentes primeiro
        Map<Long, List<Agendamento>> respondidos = agendamentoRepository
                .findByRespondidoEmNotNullOrderByRespondidoEmDesc().stream()
                .collect(groupingBy(Agendamento::getMentorId));
        Map<Long, List<Agendamento>> recebidos = agendamentoRepository
                .findByCriadoEmGreaterThanEqual(agora.minus(Duration.ofDays(JANELA_CONFIABILIDADE_DIAS))).stream()
                .collect(groupingBy(Agendamento::getMentorId));
        Set<Long> recusaramEstaStartup = recusaramEstaStartup(startupId, agora);
        Set<Long> comCertificacao = new HashSet<>(certificacaoRepository.mentoresComCertificacao());
        Map<Long, Instant> ultimoAcesso = ultimoAcessoPorMentor();
        Map<Long, Long> realizadas = new HashMap<>();
        for (Object[] linha : agendamentoRepository.realizadasPorMentor(agoraLocal)) {
            realizadas.put((Long) linha[0], ((Number) linha[1]).longValue());
        }

        // Soma e quantidade das notas de cada mentor (a consulta devolve a média; soma = média × quantidade)
        Map<Long, double[]> notas = new HashMap<>();
        double somaGeral = 0;
        long totalGeral = 0;
        for (Object[] linha : avaliacaoRepository.resumoPorMentor()) {
            long quantidade = ((Number) linha[2]).longValue();
            double soma = ((Number) linha[1]).doubleValue() * quantidade;
            notas.put((Long) linha[0], new double[] {soma, quantidade});
            somaGeral += soma;
            totalGeral += quantidade;
        }

        List<Indicadores> indicadores = mentorRepository.findAll().stream().map(m -> {
            Long id = m.getId();
            Agenda agenda = agenda(m, faixas.getOrDefault(id, List.of()), ocupando.getOrDefault(id, List.of()),
                    agoraLocal, fimJanela, fimSemana);
            List<Double> tempos = respondidos.getOrDefault(id, List.of()).stream()
                    .limit(ULTIMOS_PEDIDOS_RESPOSTA)
                    .map(IndicadoresMentorService::horasAteResponder)
                    .toList();
            double[] nota = notas.getOrDefault(id, new double[] {0, 0});
            return new Indicadores(
                    id,
                    compatibilidades.getOrDefault(id, 0),
                    agenda.ocupacao(),
                    agenda.temHorario7d(),
                    agenda.temHorario14d(),
                    mediana(tempos),
                    tempos.size(),
                    taxaConfiabilidade(recebidos.getOrDefault(id, List.of())),
                    nota[0],
                    (long) nota[1],
                    itensDoPerfil(m, comCertificacao.contains(id)),
                    ultimoAcesso.get(id),
                    m.getCriadoEm(),
                    realizadas.getOrDefault(id, 0L),
                    recusaramEstaStartup.contains(id));
        }).toList();

        return new Coleta(indicadores, RankingMentores.mediaGeral(somaGeral, totalGeral));
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

    // ---------------------------------------------------------------- startup, perfil e acesso

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
