package com.ponti.api.service;

import static com.ponti.api.config.RelogioConfig.FUSO;

import com.ponti.api.dto.DashboardAdmin;
import com.ponti.api.dto.DashboardAdmin.*;
import com.ponti.api.model.*;
import com.ponti.api.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Calcula o dashboard do admin. Lê tudo de uma vez e conta em memória: com o tamanho da Ponti
// isso é rápido e deixa cada regra fácil de ler (e de conferir à mão nos testes).
//
// Regras (os mesmos nomes aparecem na tela):
//  - Funil: das startups CADASTRADAS no período, quantas já chegaram a cada etapa (até hoje).
//    Interesse = mandou ou recebeu convite, ou pediu mentoria. Match = convite aceito ou pedido confirmado.
//    Mentoria = sessão confirmada que já aconteceu. Acompanhamento = acompanhamento aceito (ativo ou encerrado).
//    Cada etapa só conta quem também passou pelas anteriores, para o funil nunca "subir".
//  - Prova do match: pares (startup, mentor) com interesse no período, separados pela compatibilidade
//    (alta: 70% ou mais; média: 40 a 69%; baixa: menos de 40%). Taxa = quantos desses pares viraram match.
//  - Oferta x demanda, saúde e atenção são uma foto de agora (não dependem do período).
@Service
public class DashboardService {

    static final int DIAS_PADRAO = 30;
    static final int DIAS_MAXIMO = 366;
    static final int MATCH_ALTO = 70;
    static final int MATCH_BAIXO = 40;
    static final int DIAS_PARADO = 14;
    static final int DIAS_SEM_CONEXAO = 14;
    static final int DIAS_SEM_RESPOSTA = 3;
    static final double NOTA_MINIMA = 3.5;
    static final int AVALIACOES_MINIMAS = 2;

    private final UsuarioRepository usuarioRepository;
    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;
    private final ConviteRepository conviteRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final AcompanhamentoRepository acompanhamentoRepository;
    private final AtividadeRepository atividadeRepository;
    private final EntregaRepository entregaRepository;
    private final MetaRepository metaRepository;
    private final ComentarioRepository comentarioRepository;
    private final Clock relogio;

    public DashboardService(UsuarioRepository usuarioRepository, StartupRepository startupRepository,
                            MentorRepository mentorRepository, ConviteRepository conviteRepository,
                            AgendamentoRepository agendamentoRepository, AvaliacaoRepository avaliacaoRepository,
                            AcompanhamentoRepository acompanhamentoRepository, AtividadeRepository atividadeRepository,
                            EntregaRepository entregaRepository, MetaRepository metaRepository,
                            ComentarioRepository comentarioRepository, Clock relogio) {
        this.usuarioRepository = usuarioRepository;
        this.startupRepository = startupRepository;
        this.mentorRepository = mentorRepository;
        this.conviteRepository = conviteRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.acompanhamentoRepository = acompanhamentoRepository;
        this.atividadeRepository = atividadeRepository;
        this.entregaRepository = entregaRepository;
        this.metaRepository = metaRepository;
        this.comentarioRepository = comentarioRepository;
        this.relogio = relogio;
    }

    // Um intervalo de dias (inclusive nas duas pontas), no fuso de Recife
    record Janela(LocalDate inicio, LocalDate fim) {
        boolean contem(Instant instante) {
            if (instante == null) return false;
            LocalDate dia = instante.atZone(FUSO).toLocalDate();
            return !dia.isBefore(inicio) && !dia.isAfter(fim);
        }

        boolean contem(LocalDateTime momento) {
            if (momento == null) return false;
            LocalDate dia = momento.toLocalDate();
            return !dia.isBefore(inicio) && !dia.isAfter(fim);
        }
    }

    // Tudo o que o dashboard precisa, lido uma vez só
    private record Base(List<Usuario> usuarios, Map<Long, Startup> startups, Map<Long, Mentor> mentores,
                        List<Convite> convites, List<Agendamento> agendamentos, List<Avaliacao> avaliacoes,
                        List<Acompanhamento> acompanhamentos, Set<Long> startupsBloqueadas,
                        Set<Long> mentoresBloqueados, LocalDateTime agora) {
    }

    @Transactional(readOnly = true)
    public DashboardAdmin gerar(LocalDate inicio, LocalDate fim) {
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate f = fim == null ? hoje : fim;
        LocalDate i = inicio == null ? f.minusDays(DIAS_PADRAO - 1L) : inicio;
        if (i.isAfter(f)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial precisa ser antes da final");
        }
        long dias = ChronoUnit.DAYS.between(i, f) + 1;
        if (dias > DIAS_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha um período de até 1 ano");
        }
        Janela atual = new Janela(i, f);
        Janela anterior = new Janela(i.minusDays(dias), i.minusDays(1));

        Base b = carregar();
        return new DashboardAdmin(
                new Periodo(atual.inicio(), atual.fim(), anterior.inicio(), anterior.fim()),
                destaques(b, atual, anterior),
                funil(b, atual, anterior),
                provaDoMatch(b, atual, anterior),
                ofertaDemanda(b),
                saude(b),
                atencao(b));
    }

    private Base carregar() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        Set<Long> startupsBloqueadas = new HashSet<>();
        Set<Long> mentoresBloqueados = new HashSet<>();
        for (Usuario u : usuarios) {
            if (!u.estaBloqueado()) continue;
            if (u.getStartupId() != null) startupsBloqueadas.add(u.getStartupId());
            if (u.getMentorId() != null) mentoresBloqueados.add(u.getMentorId());
        }
        return new Base(usuarios,
                startupRepository.findAll().stream().collect(Collectors.toMap(Startup::getId, s -> s)),
                mentorRepository.findAll().stream().collect(Collectors.toMap(Mentor::getId, m -> m)),
                conviteRepository.findAll(), agendamentoRepository.findAll(), avaliacaoRepository.findAll(),
                acompanhamentoRepository.findAll(), startupsBloqueadas, mentoresBloqueados,
                LocalDateTime.now(relogio));
    }

    // ---------------------------------------------------------------- destaques

    private List<Destaque> destaques(Base b, Janela atual, Janela anterior) {
        return List.of(
                new Destaque("startups", "Startups cadastradas", cadastros(b, TipoUsuario.STARTUP, atual),
                        cadastros(b, TipoUsuario.STARTUP, anterior)),
                new Destaque("mentores", "Mentores cadastrados", cadastros(b, TipoUsuario.MENTOR, atual),
                        cadastros(b, TipoUsuario.MENTOR, anterior)),
                new Destaque("matches", "Novos matches", matches(b, atual), matches(b, anterior)),
                new Destaque("mentorias", "Mentorias realizadas", mentoriasRealizadas(b, atual),
                        mentoriasRealizadas(b, anterior)));
    }

    private long cadastros(Base b, TipoUsuario tipo, Janela j) {
        return b.usuarios().stream().filter(u -> u.getTipo() == tipo && j.contem(u.getCriadoEm())).count();
    }

    private long matches(Base b, Janela j) {
        long convites = b.convites().stream()
                .filter(c -> c.getStatus() == StatusConvite.ACEITO && j.contem(c.getRespondidoEm())).count();
        long pedidos = b.agendamentos().stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONFIRMADO && j.contem(a.getRespondidoEm())).count();
        return convites + pedidos;
    }

    private long mentoriasRealizadas(Base b, Janela j) {
        return b.agendamentos().stream().filter(a -> realizada(a, b.agora()) && j.contem(a.getFim())).count();
    }

    // ---------------------------------------------------------------- funil

    private Funil funil(Base b, Janela atual, Janela anterior) {
        long[] agora = etapas(b, atual);
        long[] antes = etapas(b, anterior);
        String[][] nomes = {
                {"CADASTRO", "Cadastro"}, {"INTERESSE", "Interesse"}, {"MATCH", "Match"},
                {"MENTORIA", "Mentoria"}, {"ACOMPANHAMENTO", "Acompanhamento"}};
        List<EtapaFunil> etapas = new ArrayList<>();
        String maiorQueda = null;
        Integer menorTaxa = null;
        for (int k = 0; k < nomes.length; k++) {
            Integer taxa = k == 0 ? null : porcentagem(agora[k], agora[k - 1]);
            etapas.add(new EtapaFunil(nomes[k][0], nomes[k][1], agora[k], taxa, porcentagem(agora[k], agora[0]),
                    antes[k], k == 0 ? null : porcentagem(antes[k], antes[k - 1])));
            if (taxa != null && (menorTaxa == null || taxa < menorTaxa)) {
                menorTaxa = taxa;
                maiorQueda = nomes[k][0];
            }
        }
        return new Funil(etapas, maiorQueda);
    }

    // Quantidade de startups em cada etapa, das cadastradas na janela
    private long[] etapas(Base b, Janela j) {
        Set<Long> coorte = b.usuarios().stream()
                .filter(u -> u.getTipo() == TipoUsuario.STARTUP && u.getStartupId() != null && j.contem(u.getCriadoEm()))
                .map(Usuario::getStartupId).collect(Collectors.toSet());

        Set<Long> comInteresse = new HashSet<>();
        Set<Long> comMatch = new HashSet<>();
        Set<Long> comMentoria = new HashSet<>();
        b.convites().forEach(c -> {
            comInteresse.add(c.getStartupId());
            if (c.getStatus() == StatusConvite.ACEITO) comMatch.add(c.getStartupId());
        });
        b.agendamentos().forEach(a -> {
            comInteresse.add(a.getStartupId());
            if (a.getStatus() == StatusAgendamento.CONFIRMADO) comMatch.add(a.getStartupId());
            if (realizada(a, b.agora())) comMentoria.add(a.getStartupId());
        });
        Set<Long> comAcompanhamento = b.acompanhamentos().stream()
                .filter(a -> a.getStatus() == StatusAcompanhamento.ATIVO || a.getStatus() == StatusAcompanhamento.ENCERRADO)
                .map(Acompanhamento::getStartupId).collect(Collectors.toSet());

        List<Set<Long>> condicoes = List.of(comInteresse, comMatch, comMentoria, comAcompanhamento);
        long[] resultado = new long[5];
        Set<Long> restantes = new HashSet<>(coorte);
        resultado[0] = restantes.size();
        for (int k = 0; k < condicoes.size(); k++) {
            restantes.retainAll(condicoes.get(k));
            resultado[k + 1] = restantes.size();
        }
        return resultado;
    }

    // ---------------------------------------------------------------- prova do match

    private ProvaDoMatch provaDoMatch(Base b, Janela atual, Janela anterior) {
        Map<String, int[]> agora = faixas(b, atual);
        Map<String, int[]> antes = faixas(b, anterior);
        String[][] nomes = {{"ALTA", "70% ou mais"}, {"MEDIA", "De 40% a 69%"}, {"BAIXA", "Menos de 40%"}};
        List<FaixaMatch> faixas = new ArrayList<>();
        for (String[] n : nomes) {
            int[] a = agora.get(n[0]);
            int[] p = antes.get(n[0]);
            faixas.add(new FaixaMatch(n[0], n[1], a[0], a[1], porcentagem(a[1], a[0]), p[0], porcentagem(p[1], p[0])));
        }
        Integer alta = faixas.get(0).taxa();
        Integer baixa = faixas.get(2).taxa();
        return new ProvaDoMatch(faixas, alta == null || baixa == null ? null : alta - baixa);
    }

    // Para cada faixa: [pares com interesse na janela, pares que viraram match]
    private Map<String, int[]> faixas(Base b, Janela j) {
        Set<List<Long>> pares = new LinkedHashSet<>();
        b.convites().stream().filter(c -> j.contem(c.getCriadoEm()))
                .forEach(c -> pares.add(List.of(c.getStartupId(), c.getMentorId())));
        b.agendamentos().stream().filter(a -> j.contem(a.getCriadoEm()))
                .forEach(a -> pares.add(List.of(a.getStartupId(), a.getMentorId())));

        Set<List<Long>> convertidos = new HashSet<>();
        b.convites().stream().filter(c -> c.getStatus() == StatusConvite.ACEITO)
                .forEach(c -> convertidos.add(List.of(c.getStartupId(), c.getMentorId())));
        b.agendamentos().stream().filter(a -> a.getStatus() == StatusAgendamento.CONFIRMADO)
                .forEach(a -> convertidos.add(List.of(a.getStartupId(), a.getMentorId())));

        Map<String, int[]> resultado = new HashMap<>(Map.of("ALTA", new int[2], "MEDIA", new int[2], "BAIXA", new int[2]));
        for (List<Long> par : pares) {
            Startup s = b.startups().get(par.get(0));
            Mentor m = b.mentores().get(par.get(1));
            if (s == null || m == null) continue; // conta excluída
            int compatibilidade = compatibilidade(s, m);
            int[] faixa = resultado.get(faixa(compatibilidade));
            faixa[0]++;
            if (convertidos.contains(par)) faixa[1]++;
        }
        return resultado;
    }

    static int compatibilidade(Startup s, Mentor m) {
        return MatchService.calcular(s.getNecessidades(), s.getSegmento(), m.getTemas(), m.getEcossistemas(), true)
                .pontos();
    }

    public static String faixa(int compatibilidade) {
        if (compatibilidade >= MATCH_ALTO) return "ALTA";
        return compatibilidade >= MATCH_BAIXO ? "MEDIA" : "BAIXA";
    }

    // ---------------------------------------------------------------- oferta x demanda

    private List<TemaOfertaDemanda> ofertaDemanda(Base b) {
        List<TemaOfertaDemanda> lista = new ArrayList<>();
        for (Tema tema : Tema.values()) {
            int startups = (int) b.startups().values().stream()
                    .filter(s -> !b.startupsBloqueadas().contains(s.getId()))
                    .filter(s -> s.getNecessidades().contains(tema)).count();
            int mentores = (int) b.mentores().values().stream()
                    .filter(m -> !b.mentoresBloqueados().contains(m.getId()))
                    .filter(m -> m.getTemas().contains(tema)).count();
            if (startups == 0 && mentores == 0) continue;
            lista.add(new TemaOfertaDemanda(tema, tema.getRotulo(), startups, mentores, situacao(startups, mentores)));
        }
        // Primeiro os temas em que mais falta mentor (startups por mentor, do maior para o menor)
        lista.sort(Comparator.comparingDouble((TemaOfertaDemanda t) -> -(double) t.startups() / Math.max(t.mentores(), 0.5))
                .thenComparing(t -> t.tema().name()));
        return lista;
    }

    // FALTA: 1,5 startup ou mais por mentor (ou nenhum mentor). SOBRA: 2 mentores ou mais por startup.
    public static String situacao(int startups, int mentores) {
        if (startups > 0 && (mentores == 0 || startups >= 1.5 * mentores)) return "FALTA";
        if (mentores >= 2 * startups) return "SOBRA";
        return "EQUILIBRIO";
    }

    // ---------------------------------------------------------------- saúde dos acompanhamentos

    private Saude saude(Base b) {
        List<Acompanhamento> ativos = b.acompanhamentos().stream()
                .filter(a -> a.getStatus() == StatusAcompanhamento.ATIVO).toList();
        int noPrazo = 0;
        int atrasadas = 0;
        int vencidas = 0;
        List<AcompanhamentoParado> parados = new ArrayList<>();

        for (Acompanhamento ac : ativos) {
            List<Atividade> atividades = atividadeRepository.findByAcompanhamentoIdOrderByPrazoAscIdAsc(ac.getId());
            LocalDateTime ultimoMovimento = ac.getDataInicio() != null ? ac.getDataInicio().atStartOfDay()
                    : ac.getCriadoEm().atZone(FUSO).toLocalDateTime();
            for (Meta meta : metaRepository.findByAcompanhamentoIdOrderByOrdemAsc(ac.getId())) {
                if (meta.getCumpridaEm() != null) {
                    ultimoMovimento = maisRecente(ultimoMovimento, meta.getCumpridaEm().atZone(FUSO).toLocalDateTime());
                }
            }
            for (Atividade at : atividades) {
                if (at.getCriadaEm() != null) {
                    ultimoMovimento = maisRecente(ultimoMovimento, at.getCriadaEm().atZone(FUSO).toLocalDateTime());
                }
                if (at.estaAtrasada(b.agora())) vencidas++;
                List<Entrega> entregas = entregaRepository.findByAtividadeIdOrderByIdAsc(at.getId());
                if (!entregas.isEmpty()) {
                    // Vale a primeira entrega: foi ela que chegou (ou não) dentro do prazo
                    if (entregas.get(0).getEnviadaEm().isAfter(at.getPrazo())) atrasadas++;
                    else noPrazo++;
                }
                for (Entrega e : entregas) {
                    ultimoMovimento = maisRecente(ultimoMovimento, e.getEnviadaEm());
                    for (Comentario c : comentarioRepository.findByEntregaIdOrderByIdAsc(e.getId())) {
                        ultimoMovimento = maisRecente(ultimoMovimento, c.getCriadoEm());
                    }
                }
            }
            long diasParado = ChronoUnit.DAYS.between(ultimoMovimento, b.agora());
            if (diasParado >= DIAS_PARADO) {
                parados.add(new AcompanhamentoParado(ac.getId(), nomeStartup(b, ac.getStartupId()),
                        nomeMentor(b, ac.getMentorId()), diasParado));
            }
        }
        parados.sort(Comparator.comparingLong(AcompanhamentoParado::diasSemMovimento).reversed());
        return new Saude(ativos.size(), noPrazo, atrasadas, vencidas, porcentagem(noPrazo, noPrazo + atrasadas), parados);
    }

    private static LocalDateTime maisRecente(LocalDateTime a, LocalDateTime b) {
        return b != null && b.isAfter(a) ? b : a;
    }

    // ---------------------------------------------------------------- precisa de atenção

    private List<Atencao> atencao(Base b) {
        Map<Long, Usuario> porMentor = new HashMap<>();
        Map<Long, Usuario> porStartup = new HashMap<>();
        for (Usuario u : b.usuarios()) {
            if (u.getMentorId() != null) porMentor.put(u.getMentorId(), u);
            if (u.getStartupId() != null) porStartup.put(u.getStartupId(), u);
        }
        List<Atencao> lista = new ArrayList<>();

        // Mentores mal avaliados (pior nota primeiro)
        Map<Long, List<Avaliacao>> porNota = b.avaliacoes().stream()
                .collect(Collectors.groupingBy(Avaliacao::getMentorId));
        porNota.entrySet().stream()
                .filter(e -> e.getValue().size() >= AVALIACOES_MINIMAS)
                .map(e -> Map.entry(e.getKey(), e.getValue().stream().mapToInt(Avaliacao::getNota).average().orElse(5)))
                .filter(e -> e.getValue() < NOTA_MINIMA)
                .sorted(Map.Entry.comparingByValue())
                .forEach(e -> adicionarMentor(lista, b, porMentor, e.getKey(), "MENTOR_MAL_AVALIADO",
                        String.format(Locale.forLanguageTag("pt-BR"), "Nota média %.1f em %d avaliações",
                                e.getValue(), porNota.get(e.getKey()).size())));

        // Mentores com pedidos ou convites da startup esperando resposta há mais de 3 dias
        LocalDateTime limite = b.agora().minusDays(DIAS_SEM_RESPOSTA);
        Map<Long, List<Instant>> esperando = new HashMap<>();
        b.agendamentos().stream().filter(a -> a.getStatus() == StatusAgendamento.PENDENTE)
                .forEach(a -> esperando.computeIfAbsent(a.getMentorId(), k -> new ArrayList<>()).add(a.getCriadoEm()));
        b.convites().stream()
                .filter(c -> c.getStatus() == StatusConvite.PENDENTE && c.getEnviadoPor() == TipoUsuario.STARTUP)
                .forEach(c -> esperando.computeIfAbsent(c.getMentorId(), k -> new ArrayList<>()).add(c.getCriadoEm()));
        esperando.forEach((mentorId, datas) -> {
            List<LocalDateTime> antigos = datas.stream().filter(Objects::nonNull)
                    .map(d -> d.atZone(FUSO).toLocalDateTime()).filter(d -> d.isBefore(limite)).sorted().toList();
            if (antigos.isEmpty()) return;
            long dias = ChronoUnit.DAYS.between(antigos.get(0), b.agora());
            String motivo = antigos.size() == 1
                    ? "1 pedido sem resposta há " + dias + " dias"
                    : antigos.size() + " pedidos sem resposta, o mais antigo há " + dias + " dias";
            adicionarMentor(lista, b, porMentor, mentorId, "MENTOR_SEM_RESPOSTA", motivo);
        });

        // Startups cadastradas há mais de 14 dias e ainda sem nenhum match
        Set<Long> comMatch = new HashSet<>();
        b.convites().stream().filter(c -> c.getStatus() == StatusConvite.ACEITO).forEach(c -> comMatch.add(c.getStartupId()));
        b.agendamentos().stream().filter(a -> a.getStatus() == StatusAgendamento.CONFIRMADO)
                .forEach(a -> comMatch.add(a.getStartupId()));
        Instant cadastroLimite = b.agora().minusDays(DIAS_SEM_CONEXAO).atZone(FUSO).toInstant();
        b.usuarios().stream()
                .filter(u -> u.getStartupId() != null && !u.estaBloqueado() && !comMatch.contains(u.getStartupId()))
                .filter(u -> u.getCriadoEm() != null && u.getCriadoEm().isBefore(cadastroLimite))
                .sorted(Comparator.comparing(Usuario::getCriadoEm))
                .forEach(u -> {
                    long dias = ChronoUnit.DAYS.between(u.getCriadoEm().atZone(FUSO).toLocalDateTime(), b.agora());
                    long tentativas = b.convites().stream().filter(c -> u.getStartupId().equals(c.getStartupId())).count()
                            + b.agendamentos().stream().filter(a -> u.getStartupId().equals(a.getStartupId())).count();
                    String motivo = "Cadastrada há " + dias + " dias e ainda sem conexão"
                            + (tentativas == 0 ? " (não pediu nenhuma mentoria)" : " (" + tentativas + " tentativa"
                            + (tentativas > 1 ? "s" : "") + " sem sucesso)");
                    lista.add(new Atencao("STARTUP_SEM_CONEXAO", u.getId(), TipoUsuario.STARTUP, u.getStartupId(),
                            nomeStartup(b, u.getStartupId()), motivo, false));
                });
        return lista;
    }

    private void adicionarMentor(List<Atencao> lista, Base b, Map<Long, Usuario> porMentor, Long mentorId,
                                 String tipo, String motivo) {
        Usuario u = porMentor.get(mentorId);
        if (u == null || u.estaBloqueado() || !b.mentores().containsKey(mentorId)) return;
        lista.add(new Atencao(tipo, u.getId(), TipoUsuario.MENTOR, mentorId, nomeMentor(b, mentorId), motivo, false));
    }

    // ---------------------------------------------------------------- apoio

    private static boolean realizada(Agendamento a, LocalDateTime agora) {
        return a.getStatus() == StatusAgendamento.CONFIRMADO && a.getFim() != null && a.getFim().isBefore(agora);
    }

    static Integer porcentagem(long parte, long total) {
        return total == 0 ? null : (int) Math.round(100.0 * parte / total);
    }

    private static String nomeStartup(Base b, Long id) {
        Startup s = b.startups().get(id);
        return s == null ? "Startup excluída" : s.getNome();
    }

    private static String nomeMentor(Base b, Long id) {
        Mentor m = b.mentores().get(id);
        return m == null ? "Mentor excluído" : m.getNome();
    }
}
