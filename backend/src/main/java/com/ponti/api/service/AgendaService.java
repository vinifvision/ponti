package com.ponti.api.service;

import com.ponti.api.dto.AgendamentoDetalhe;
import com.ponti.api.dto.FaixaDisponibilidade;
import com.ponti.api.dto.NovoAgendamento;
import com.ponti.api.model.Agendamento;
import com.ponti.api.model.Disponibilidade;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.StatusAgendamento;
import com.ponti.api.model.StatusConvite;
import com.ponti.api.model.StatusPagamento;
import com.ponti.api.model.Startup;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.ConviteRepository;
import com.ponti.api.repository.DisponibilidadeRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Regras da agenda: disponibilidade semanal do mentor, horários livres e o ciclo de vida do agendamento.
@Service
public class AgendaService {

    // Duração usada quando o mentor ainda não definiu a dele
    public static final int DURACAO_PADRAO_MINUTOS = 60;
    // Período máximo que dá para consultar de uma vez (o calendário mostra um mês)
    static final int MAXIMO_DIAS_CONSULTA = 62;
    // Status que ocupam um horário na agenda do mentor
    static final List<StatusAgendamento> OCUPAM_HORARIO = List.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO);

    private final DisponibilidadeRepository disponibilidadeRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final MentorRepository mentorRepository;
    private final StartupRepository startupRepository;
    private final ConviteRepository conviteRepository;
    private final Clock relogio;

    public AgendaService(DisponibilidadeRepository disponibilidadeRepository,
                         AgendamentoRepository agendamentoRepository,
                         MentorRepository mentorRepository,
                         StartupRepository startupRepository,
                         ConviteRepository conviteRepository,
                         Clock relogio) {
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.mentorRepository = mentorRepository;
        this.startupRepository = startupRepository;
        this.conviteRepository = conviteRepository;
        this.relogio = relogio;
    }

    // ---------------------------------------------------------------- disponibilidade

    public List<FaixaDisponibilidade> disponibilidade(Long mentorId) {
        buscarMentor(mentorId);
        // Ordena em Java: no banco o dia é texto, e "THURSDAY" viria antes de "TUESDAY"
        return disponibilidadeRepository.findByMentorId(mentorId).stream()
                .map(FaixaDisponibilidade::de)
                .sorted(Comparator.comparing(FaixaDisponibilidade::diaSemana)
                        .thenComparing(FaixaDisponibilidade::horaInicio))
                .toList();
    }

    // Substitui a disponibilidade inteira do mentor pela lista enviada (lista vazia = não atende)
    @Transactional
    public List<FaixaDisponibilidade> definirDisponibilidade(Long mentorId, List<FaixaDisponibilidade> faixas) {
        buscarMentor(mentorId);
        garantirSemSobreposicao(faixas);

        disponibilidadeRepository.deleteByMentorId(mentorId);
        disponibilidadeRepository.flush();
        for (FaixaDisponibilidade faixa : faixas) {
            Disponibilidade d = new Disponibilidade();
            d.setMentorId(mentorId);
            d.setDiaSemana(faixa.diaSemana());
            d.setHoraInicio(faixa.horaInicio());
            d.setHoraFim(faixa.horaFim());
            disponibilidadeRepository.save(d);
        }
        return disponibilidade(mentorId);
    }

    private void garantirSemSobreposicao(List<FaixaDisponibilidade> faixas) {
        List<FaixaDisponibilidade> ordenadas = faixas.stream()
                .sorted(Comparator.comparing(FaixaDisponibilidade::diaSemana)
                        .thenComparing(FaixaDisponibilidade::horaInicio))
                .toList();
        for (int i = 1; i < ordenadas.size(); i++) {
            FaixaDisponibilidade anterior = ordenadas.get(i - 1);
            FaixaDisponibilidade atual = ordenadas.get(i);
            if (anterior.diaSemana() == atual.diaSemana() && atual.horaInicio().isBefore(anterior.horaFim())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Há faixas de horário que se sobrepõem no mesmo dia");
            }
        }
    }

    // ---------------------------------------------------------------- horários livres

    // Horários em que a startup pode marcar, entre as datas (inclusive): saem da disponibilidade
    // semanal, em blocos do tamanho da sessão do mentor, sem os que já passaram ou estão ocupados.
    public List<LocalDateTime> horariosLivres(Long mentorId, LocalDate de, LocalDate ate) {
        Mentor mentor = buscarMentor(mentorId);
        if (ate.isBefore(de)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final precisa ser igual ou depois da inicial");
        }
        if (ChronoUnit.DAYS.between(de, ate) > MAXIMO_DIAS_CONSULTA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Consulte no máximo " + MAXIMO_DIAS_CONSULTA + " dias por vez");
        }

        int duracao = duracao(mentor);
        LocalDateTime agora = LocalDateTime.now(relogio);
        List<Disponibilidade> faixas = disponibilidadeRepository.findByMentorId(mentorId);
        List<Agendamento> ocupados = agendamentoRepository.ocupandoHorario(
                mentorId, OCUPAM_HORARIO, de.atStartOfDay(), ate.plusDays(1).atStartOfDay());

        List<LocalDateTime> livres = new ArrayList<>();
        for (LocalDate dia = de; !dia.isAfter(ate); dia = dia.plusDays(1)) {
            for (Disponibilidade faixa : faixas) {
                if (faixa.getDiaSemana() != dia.getDayOfWeek()) {
                    continue;
                }
                LocalDateTime inicio = dia.atTime(faixa.getHoraInicio());
                LocalDateTime limite = dia.atTime(faixa.getHoraFim());
                while (!inicio.plusMinutes(duracao).isAfter(limite)) {
                    LocalDateTime fim = inicio.plusMinutes(duracao);
                    if (inicio.isAfter(agora) && !conflita(inicio, fim, ocupados)) {
                        livres.add(inicio);
                    }
                    inicio = fim;
                }
            }
        }
        livres.sort(Comparator.naturalOrder());
        return livres;
    }

    private boolean conflita(LocalDateTime inicio, LocalDateTime fim, List<Agendamento> ocupados) {
        return ocupados.stream().anyMatch(a -> inicio.isBefore(a.getFim()) && fim.isAfter(a.getInicio()));
    }

    // ---------------------------------------------------------------- agendamentos

    // A startup marca um horário livre. Pagamento simulado: se a mentoria é paga, basta escolher o método.
    @Transactional
    public Agendamento agendar(NovoAgendamento pedido) {
        Startup startup = startupRepository.findById(pedido.startupId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrada"));
        Mentor mentor = buscarMentor(pedido.mentorId());

        LocalDateTime inicio = pedido.inicio().truncatedTo(ChronoUnit.MINUTES);
        LocalDate dia = inicio.toLocalDate();
        if (!horariosLivres(mentor.getId(), dia, dia).contains(inicio)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse horário não está mais disponível. Escolha outro no calendário.");
        }

        BigDecimal valor = mentor.getValorSessao() == null ? BigDecimal.ZERO : mentor.getValorSessao();
        boolean gratuita = valor.signum() == 0;
        if (!gratuita && pedido.metodoPagamento() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha a forma de pagamento");
        }

        Agendamento agendamento = new Agendamento();
        agendamento.setStartupId(startup.getId());
        agendamento.setMentorId(mentor.getId());
        agendamento.setInicio(inicio);
        agendamento.setFim(inicio.plusMinutes(duracao(mentor)));
        agendamento.setValor(valor);
        agendamento.setStatus(StatusAgendamento.PENDENTE);
        agendamento.setStatusPagamento(gratuita ? StatusPagamento.GRATUITA : StatusPagamento.PAGO);
        agendamento.setMetodoPagamento(gratuita ? null : pedido.metodoPagamento());
        agendamento.setMensagem(pedido.mensagem());
        Agendamento salvo = agendamentoRepository.save(agendamento);

        aceitarSugestaoDoMentor(startup.getId(), mentor.getId());
        return salvo;
    }

    // Se o mentor tinha sugerido uma mentoria para essa startup, agendar responde a sugestão
    private void aceitarSugestaoDoMentor(Long startupId, Long mentorId) {
        conviteRepository.findByStartupId(startupId).stream()
                .filter(c -> mentorId.equals(c.getMentorId()))
                .filter(c -> c.getEnviadoPor() == TipoUsuario.MENTOR && c.getStatus() == StatusConvite.PENDENTE)
                .forEach(c -> {
                    c.setStatus(StatusConvite.ACEITO);
                    conviteRepository.save(c);
                });
    }

    // O mentor confirma o pedido e informa o link da reunião
    @Transactional
    public Agendamento confirmar(Long id, String linkReuniao) {
        Agendamento agendamento = buscar(id);
        exigirStatus(agendamento, StatusAgendamento.PENDENTE, "Só pedidos pendentes podem ser confirmados");
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamento.setLinkReuniao(linkReuniao.trim());
        return agendamentoRepository.save(agendamento);
    }

    // O mentor recusa o pedido; se foi pago, o valor é "reembolsado" (simulado)
    @Transactional
    public Agendamento recusar(Long id) {
        Agendamento agendamento = buscar(id);
        exigirStatus(agendamento, StatusAgendamento.PENDENTE, "Só pedidos pendentes podem ser recusados");
        agendamento.setStatus(StatusAgendamento.RECUSADO);
        reembolsarSePago(agendamento);
        return agendamentoRepository.save(agendamento);
    }

    // Qualquer lado cancela uma sessão pendente ou confirmada que ainda não aconteceu
    @Transactional
    public Agendamento cancelar(Long id) {
        Agendamento agendamento = buscar(id);
        if (agendamento.getStatus() != StatusAgendamento.PENDENTE && agendamento.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa mentoria não pode mais ser cancelada");
        }
        if (!agendamento.getInicio().isAfter(LocalDateTime.now(relogio))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa mentoria já começou ou já aconteceu");
        }
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        reembolsarSePago(agendamento);
        return agendamentoRepository.save(agendamento);
    }

    public List<AgendamentoDetalhe> listar(Long startupId, Long mentorId) {
        List<Agendamento> lista;
        if (mentorId != null) {
            lista = agendamentoRepository.findByMentorIdOrderByInicioAsc(mentorId);
        } else if (startupId != null) {
            lista = agendamentoRepository.findByStartupIdOrderByInicioAsc(startupId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe startupId ou mentorId");
        }
        return lista.stream().map(this::detalhar).toList();
    }

    public AgendamentoDetalhe detalhar(Agendamento a) {
        Startup s = startupRepository.findById(a.getStartupId()).orElse(null);
        Mentor m = mentorRepository.findById(a.getMentorId()).orElse(null);
        return new AgendamentoDetalhe(
                a.getId(), a.getInicio(), a.getFim(), a.getValor(), a.getStatus(), a.getStatusPagamento(),
                a.getMetodoPagamento(),
                a.getStatus() == StatusAgendamento.CONFIRMADO ? a.getLinkReuniao() : null,
                a.getMensagem(),
                s == null ? null : new AgendamentoDetalhe.Parte(s.getId(), s.getNome(), s.getFotoUrl(), s.getSegmento()),
                m == null ? null : new AgendamentoDetalhe.Parte(m.getId(), m.getNome(), m.getFotoUrl(), m.getAreasAtuacao()));
    }

    // ---------------------------------------------------------------- apoio

    private void reembolsarSePago(Agendamento agendamento) {
        if (agendamento.getStatusPagamento() == StatusPagamento.PAGO) {
            agendamento.setStatusPagamento(StatusPagamento.REEMBOLSADO);
        }
    }

    private void exigirStatus(Agendamento agendamento, StatusAgendamento esperado, String mensagem) {
        if (agendamento.getStatus() != esperado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
        }
    }

    public Agendamento buscar(Long id) {
        return agendamentoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado"));
    }

    private Mentor buscarMentor(Long mentorId) {
        return mentorRepository.findById(mentorId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado(a)"));
    }

    private int duracao(Mentor mentor) {
        Integer minutos = mentor.getDuracaoSessaoMinutos();
        return minutos == null ? DURACAO_PADRAO_MINUTOS : minutos;
    }
}
