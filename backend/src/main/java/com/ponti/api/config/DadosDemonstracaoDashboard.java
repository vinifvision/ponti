package com.ponti.api.config;

import com.ponti.api.model.*;
import com.ponti.api.repository.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Dados de DEMONSTRAÇÃO do dashboard do admin (dados SIMULADOS, para o pitch).
// Os outros conjuntos mostram o app "em uso"; este cria jornadas variadas para cada bloco do dashboard
// ter o que mostrar:
//   - funil: 12 startups novas espalhadas pelos últimos 90 dias, que param em etapas diferentes;
//   - prova do match: pares de match alto que viram mentoria e pares de match baixo que são recusados;
//   - oferta x demanda: muita procura por Marketing, Tecnologia e Jurídico, poucos mentores nesses temas;
//   - saúde dos acompanhamentos: um acompanhamento parado e entregas no prazo e atrasadas;
//   - precisa de atenção: 2 mentores mal avaliados (um demora para responder) e startups sem conexão.
//
// Também preenche a data de cadastro das contas antigas (a coluna é nova): estima pela primeira
// atividade de cada conta. Roda depois dos outros conjuntos e só uma vez (confere rotaverde@exemplo.com).
// Logins extras (senha ponti1234): henrique@ e sergio@ (mentores); rotaverde@, feiraja@, clinicafacil@,
// bairroseguro@, docelar@, aprendemais@, solarcasa@, contacerta@, hortaurbana@, modacircular@, vetnorte@,
// pescaviva@, cuidaidoso@ (startups), todos @exemplo.com
@Component
@Order(4)
@ConditionalOnProperty(name = "ponti.demo.completo", havingValue = "true", matchIfMissing = true)
public class DadosDemonstracaoDashboard implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracaoDashboard.class);
    private static final ZoneId FUSO = ZoneId.of("America/Recife");
    static final String MARCADOR = "rotaverde@exemplo.com";

    private final UsuarioRepository usuarioRepository;
    private final MentorRepository mentorRepository;
    private final StartupRepository startupRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final ConviteRepository conviteRepository;
    private final AcompanhamentoRepository acompanhamentoRepository;
    private final MetaRepository metaRepository;
    private final AtividadeRepository atividadeRepository;
    private final EntregaRepository entregaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock relogio;

    private final Map<String, Mentor> mentores = new HashMap<>();
    private final Map<String, Startup> startups = new HashMap<>();
    private LocalDateTime agora;
    private int sequencia = 1;
    private int ordemMeta;

    public DadosDemonstracaoDashboard(UsuarioRepository usuarioRepository, MentorRepository mentorRepository,
                                      StartupRepository startupRepository, AgendamentoRepository agendamentoRepository,
                                      AvaliacaoRepository avaliacaoRepository, ConviteRepository conviteRepository,
                                      AcompanhamentoRepository acompanhamentoRepository, MetaRepository metaRepository,
                                      AtividadeRepository atividadeRepository, EntregaRepository entregaRepository,
                                      PasswordEncoder passwordEncoder, Clock relogio) {
        this.usuarioRepository = usuarioRepository;
        this.mentorRepository = mentorRepository;
        this.startupRepository = startupRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.conviteRepository = conviteRepository;
        this.acompanhamentoRepository = acompanhamentoRepository;
        this.metaRepository = metaRepository;
        this.atividadeRepository = atividadeRepository;
        this.entregaRepository = entregaRepository;
        this.passwordEncoder = passwordEncoder;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(String... args) {
        agora = LocalDateTime.now(relogio).withSecond(0).withNano(0);
        boolean primeiraVez = usuarioRepository.findByEmail(MARCADOR).isEmpty();

        // Sempre: contas e convites sem data (registros antigos) ganham uma data estimada
        preencherDatasQueFaltam(primeiraVez);
        if (!primeiraVez) {
            return;
        }
        for (String chave : List.of("juliana", "camila", "rafael", "beatriz", "thiago", "fernanda", "marcos",
                "anapaula", "diego", "lucas")) {
            usuarioRepository.findByEmail(chave + "@exemplo.com").map(Usuario::getMentorId)
                    .flatMap(mentorRepository::findById).ifPresent(m -> mentores.put(chave, m));
        }
        usuarioRepository.findByEmail("agrosense@exemplo.com").map(Usuario::getStartupId)
                .flatMap(startupRepository::findById).ifPresent(s -> startups.put("agrosense", s));
        if (mentores.size() < 10 || !startups.containsKey("agrosense")) {
            log.info("Dados do dashboard não carregados: faltam as contas de demonstração base.");
            return;
        }
        criarMentores();
        criarStartups();
        jornadasDoUltimoMes();
        jornadasMaisAntigas();
        log.info("Dados de demonstração do dashboard carregados: {} startups e {} mentores no total",
                startupRepository.count(), mentorRepository.count());
    }

    // ---------------------------------------------------------------- datas de cadastro

    // A data de cadastro é nova no banco. Para as contas que já existiam, estima pela primeira coisa
    // que a conta fez (pedido de mentoria, convite ou acompanhamento), 7 dias antes. Mentor usa a
    // própria data de cadastro do perfil. Sem nenhuma pista, 120 dias atrás.
    // Na primeira vez, também corrige as contas @exemplo.com, que nasceram todas "agora" ao subir a API.
    private void preencherDatasQueFaltam(boolean primeiraVez) {
        Instant padrao = instante(120, "09:00");
        for (Convite c : conviteRepository.findAll()) {
            if (c.getCriadoEm() == null) {
                c.setCriadoEm(instante(3, "09:00"));
                conviteRepository.save(c);
            }
        }
        for (Usuario u : usuarioRepository.findAll()) {
            boolean exemplo = u.getEmail().endsWith("@exemplo.com");
            if (u.getCriadoEm() != null && !(primeiraVez && exemplo)) {
                continue;
            }
            Instant estimada = null;
            if (u.getMentorId() != null) {
                estimada = mentorRepository.findById(u.getMentorId()).map(Mentor::getCriadoEm).orElse(null);
            } else if (u.getStartupId() != null) {
                Long id = u.getStartupId();
                estimada = Stream.of(
                                agendamentoRepository.findByStartupIdOrderByInicioAsc(id).stream().map(Agendamento::getCriadoEm),
                                conviteRepository.findByStartupId(id).stream().map(Convite::getCriadoEm),
                                acompanhamentoRepository.findAll().stream()
                                        .filter(a -> id.equals(a.getStartupId())).map(Acompanhamento::getCriadoEm))
                        .flatMap(s -> s).filter(Objects::nonNull).min(Instant::compareTo)
                        .map(i -> i.minus(Duration.ofDays(7))).orElse(null);
            } else if (u.getCriadoEm() != null) {
                continue; // admin: mantém
            }
            u.setCriadoEm(estimada != null ? estimada : padrao);
            usuarioRepository.save(u);
        }
    }

    // ---------------------------------------------------------------- perfis

    private void criarMentores() {
        mentor("henrique", "Henrique Bastos",
                "Publicitário, ajuda startups a montar a primeira campanha e o discurso de vendas.",
                "Publicidade, Branding", "E-commerce, Marketplaces", 8, "140.00", 70,
                Tema.MARKETING, Tema.PITCH);
        mentor("sergio", "Sérgio Leal",
                "Contador e consultor financeiro de pequenas empresas.",
                "Contabilidade, Finanças", "Todas as áreas", 20, "190.00", 60,
                Tema.FINANCAS, Tema.GESTAO);
    }

    private void criarStartups() {
        // Cadastradas nos últimos 30 dias
        startup("rotaverde", "Rota Verde", "Logística reversa de embalagens com cooperativas de reciclagem.",
                "Impacto social", "Recife", 2, 26, Tema.VENDAS, Tema.MARKETING);
        startup("feiraja", "Feira Já", "Marketplace que leva os produtos das feiras livres para o celular.",
                "Marketplaces", "Olinda", 2, 22, Tema.MARKETING, Tema.PITCH);
        startup("clinicafacil", "Clínica Fácil", "Prontuário simples e agenda online para clínicas do interior.",
                "Health-techs", "Caruaru", 3, 19, Tema.JURIDICO, Tema.TECNOLOGIA);
        startup("bairroseguro", "Bairro Seguro", "Rede de alertas entre vizinhos integrada às associações de moradores.",
                "Govtech", "Jaboatão dos Guararapes", 1, 15, Tema.TECNOLOGIA, Tema.JURIDICO);
        startup("docelar", "Doce Lar", "Loja online de doces caseiros de confeiteiras da periferia.",
                "E-commerce", "Recife", 2, 12, Tema.MARKETING, Tema.VENDAS);
        startup("aprendemais", "Aprende+", "Reforço escolar por vídeo curto para o ensino fundamental.",
                "Edtechs", "Garanhuns", 1, 9, Tema.MARKETING, Tema.PRODUTO);
        startup("solarcasa", "SolarCasa", "Financiamento coletivo de placas solares para casas populares.",
                "Cleantech", "Petrolina", 1, 6, Tema.FINANCAS, Tema.JURIDICO);
        startup("contacerta", "Conta Certa", "App que divide as contas da casa entre quem mora junto.",
                "Fintechs", "Recife", 1, 3, Tema.TECNOLOGIA, Tema.MARKETING);
        startup("hortaurbana", "Horta Urbana", "Hortas em lajes e terrenos vazios, com venda por assinatura.",
                "Agritechs", "Recife", 1, 1, Tema.VENDAS, Tema.MARKETING);
        // Cadastradas antes (de 31 a 90 dias atrás)
        startup("modacircular", "Moda Circular", "Brechó online com curadoria e retirada em pontos de coleta.",
                "E-commerce", "Recife", 3, 50, Tema.MARKETING, Tema.VENDAS);
        startup("vetnorte", "VetNorte", "Atendimento veterinário a domicílio para o sertão.",
                "PetTech", "Petrolina", 2, 38, Tema.VALIDACAO, Tema.JURIDICO);
        startup("pescaviva", "Pesca Viva", "Venda direta do pescado artesanal para restaurantes.",
                "Impacto social", "Itamaracá", 2, 45, Tema.VALIDACAO, Tema.FINANCAS);
        startup("cuidaidoso", "Cuida Idoso", "Agenda compartilhada de cuidadores e familiares de idosos.",
                "Health-techs", "Recife", 3, 80, Tema.VALIDACAO, Tema.PRODUTO);
    }

    // ---------------------------------------------------------------- jornadas

    // Funil do último mês: 9 cadastros, 6 com interesse, 4 com match, 3 com mentoria e 2 em acompanhamento
    private void jornadasDoUltimoMes() {
        // Rota Verde: mentoria com a Ana Paula e acompanhamento que PAROU (nada acontece há 16 dias)
        Agendamento a = realizada("rotaverde", "anapaula", 18, "10:00");
        avaliar(a, 5, "Indicou cooperativas para o nosso piloto.");
        Acompanhamento ac = acompanhamento("anapaula", "rotaverde", StatusAcompanhamento.ATIVO,
                "Fechar parceria com 3 cooperativas de reciclagem", 16);
        meta(ac, "Mapear as cooperativas do Recife", false);
        meta(ac, "Fechar 3 parcerias", false);
        atividade(ac, "Mapear 20 cooperativas de reciclagem", -6, StatusAtividade.PENDENTE, 16);
        atividade(ac, "Montar a planilha de custo por quilo", 5, StatusAtividade.PENDENTE, 16);

        // Feira Já: mentoria com o Henrique (match alto), mas a avaliação foi ruim
        a = realizada("feiraja", "henrique", 14, "19:00");
        avaliar(a, 2, "Chegou atrasado e não tinha olhado nosso material.");

        // Clínica Fácil: convidou o Thiago, ele aceitou, teve mentoria e o acompanhamento anda bem
        convite("clinicafacil", "thiago", TipoUsuario.STARTUP, StatusConvite.ACEITO, 17, 16);
        a = realizada("clinicafacil", "thiago", 10, "15:00");
        avaliar(a, 5, "Saímos com a arquitetura do MVP desenhada.");
        ac = acompanhamento("thiago", "clinicafacil", StatusAcompanhamento.ATIVO, "Lançar o MVP do prontuário", 8);
        meta(ac, "Escolher a stack", true);
        meta(ac, "Publicar a primeira versão", false);
        Atividade at = atividade(ac, "Comparar 3 opções de banco de dados", -3, StatusAtividade.APROVADA, 8);
        entrega(at, -4);
        at = atividade(ac, "Desenhar as telas da agenda", -1, StatusAtividade.ENTREGUE, 6);
        entrega(at, 0); // entregue depois do prazo
        atividade(ac, "Configurar o deploy", 6, StatusAtividade.PENDENTE, 2);

        // Bairro Seguro: só tentou mentores de match baixo; um recusou e o outro não respondeu
        convite("bairroseguro", "lucas", TipoUsuario.STARTUP, StatusConvite.RECUSADO, 13, 12);
        convite("bairroseguro", "diego", TipoUsuario.STARTUP, StatusConvite.PENDENTE, 10, null);

        // Doce Lar: match alto com a Beatriz, mentoria já confirmada para os próximos dias
        futura("docelar", "beatriz", 4, "15:00", StatusAgendamento.CONFIRMADO, 5);

        // Aprende+: recebeu sugestão do Diego e pediu mentoria ao Henrique, que não responde há 6 dias
        convite("aprendemais", "diego", TipoUsuario.MENTOR, StatusConvite.PENDENTE, 7, null);
        futura("aprendemais", "henrique", 5, "18:00", StatusAgendamento.PENDENTE, 6);

        // SolarCasa, Conta Certa e Horta Urbana: só o cadastro

        // Tentativas fora do perfil: as startups também procuram mentores que não atendem o que elas precisam,
        // e esses pedidos quase nunca viram match (é o que a "prova do match" mostra)
        convite("rotaverde", "beatriz", TipoUsuario.STARTUP, StatusConvite.ACEITO, 21, 20);   // 75%: aceito
        convite("rotaverde", "camila", TipoUsuario.STARTUP, StatusConvite.RECUSADO, 24, 23);  // 0%
        convite("feiraja", "diego", TipoUsuario.STARTUP, StatusConvite.RECUSADO, 20, 19);     // 0%
        recusado("feiraja", "rafael", 18);                                                     // 63%
        convite("clinicafacil", "marcos", TipoUsuario.STARTUP, StatusConvite.RECUSADO, 18, 17); // 0%
        convite("clinicafacil", "fernanda", TipoUsuario.STARTUP, StatusConvite.RECUSADO, 18, 16); // 50%
        convite("bairroseguro", "camila", TipoUsuario.STARTUP, StatusConvite.RECUSADO, 12, 11); // 0%
        recusado("docelar", "thiago", 11);                                                     // 0%
        convite("aprendemais", "lucas", TipoUsuario.STARTUP, StatusConvite.RECUSADO, 8, 7);    // 13%
    }

    // Pedido de mentoria que o mentor recusou (feito "pedidoHaDias" dias atrás, respondido no dia seguinte)
    private void recusado(String startup, String mentor, int pedidoHaDias) {
        Agendamento a = futura(startup, mentor, -(pedidoHaDias - 5), "16:00", StatusAgendamento.RECUSADO, pedidoHaDias);
        a.setRespondidoEm(a.getCriadoEm().plus(Duration.ofDays(1)));
        agendamentoRepository.save(a);
    }

    // Startups mais antigas e o histórico dos mentores mal avaliados
    private void jornadasMaisAntigas() {
        avaliar(realizada("modacircular", "henrique", 40, "19:00"), 3, "Dicas genéricas, esperava mais.");
        avaliar(realizada("pescaviva", "sergio", 35, "10:00"), 2, "Não entendeu o nosso modelo de negócio.");
        avaliar(realizada("agrosense", "sergio", 30, "09:00"), 3, null);
        convite("modacircular", "beatriz", TipoUsuario.STARTUP, StatusConvite.ACEITO, 45, 44);
        avaliar(realizada("modacircular", "beatriz", 42, "14:00"), 5, "Montamos o calendário de conteúdo.");

        // VetNorte: match baixo com o Rafael (recusado) e a Fernanda recusou por falta de agenda
        convite("vetnorte", "fernanda", TipoUsuario.STARTUP, StatusConvite.RECUSADO, 33, 32);
        Agendamento recusado = futura("vetnorte", "rafael", -28, "19:00", StatusAgendamento.RECUSADO, 33);
        recusado.setRespondidoEm(recusado.getCriadoEm().plus(Duration.ofHours(30)));
        agendamentoRepository.save(recusado);

        // Mais tentativas fora do perfil no período anterior
        convite("pescaviva", "thiago", TipoUsuario.STARTUP, StatusConvite.RECUSADO, 43, 42);   // 0%
        recusado("modacircular", "camila", 47);                                                // 0%
        convite("vetnorte", "marcos", TipoUsuario.STARTUP, StatusConvite.RECUSADO, 36, 35);   // 0%

        // Cuida Idoso: match alto com a Camila, mentoria e acompanhamento já encerrado
        avaliar(realizada("cuidaidoso", "camila", 70, "10:00"), 5, "Priorizamos o MVP em uma tarde.");
        Acompanhamento ac = acompanhamento("camila", "cuidaidoso", StatusAcompanhamento.ENCERRADO,
                "Validar a agenda compartilhada com 10 famílias", 66);
        ac.setDataFim(agora.toLocalDate().minusDays(30));
        acompanhamentoRepository.save(ac);
        meta(ac, "Testar com 10 famílias", true);
        Atividade at = atividade(ac, "Entrevistar 10 famílias", -50, StatusAtividade.APROVADA, 66);
        entrega(at, -52);
        at = atividade(ac, "Ajustar o protótipo", -36, StatusAtividade.APROVADA, 50);
        entrega(at, -37);
    }

    // ---------------------------------------------------------------- apoio

    private Instant instante(int diasAtras, String hora) {
        return agora.toLocalDate().minusDays(diasAtras).atTime(LocalTime.parse(hora)).atZone(FUSO).toInstant();
    }

    private void mentor(String chave, String nome, String bio, String areas, String ecossistemas, int anos,
                        String valor, int cadastroHaDias, Tema... temas) {
        Mentor m = new Mentor();
        m.setNome(nome);
        m.setBio(bio);
        m.setAreasAtuacao(areas);
        m.setEcossistemas(ecossistemas);
        m.setAnosExperiencia(anos);
        m.setValorSessao(new BigDecimal(valor));
        m.setDuracaoSessaoMinutos(60);
        m.setTemas(DadosIniciais.temas(temas));
        m.setCriadoEm(instante(cadastroHaDias, "09:00"));
        m = mentorRepository.save(m);
        mentores.put(chave, m);
        Usuario u = usuario(chave, TipoUsuario.MENTOR, cadastroHaDias);
        u.setMentorId(m.getId());
        usuarioRepository.save(u);
    }

    private void startup(String chave, String nome, String descricao, String segmento, String cidade, int estagio,
                         int cadastroHaDias, Tema... necessidades) {
        Startup s = new Startup();
        s.setNome(nome);
        s.setDescricao(descricao);
        s.setSegmento(segmento);
        s.setCidade(cidade);
        s.setEstado("PE");
        s.setEstagioMaturidade(estagio);
        s.setNecessidades(DadosIniciais.temas(necessidades));
        s = startupRepository.save(s);
        startups.put(chave, s);
        Usuario u = usuario(chave, TipoUsuario.STARTUP, cadastroHaDias);
        u.setStartupId(s.getId());
        usuarioRepository.save(u);
    }

    private Usuario usuario(String chave, TipoUsuario tipo, int cadastroHaDias) {
        Usuario u = new Usuario();
        u.setEmail(chave + "@exemplo.com");
        u.setSenhaHash(passwordEncoder.encode(DadosIniciais.SENHA_EXEMPLO));
        u.setTipo(tipo);
        u.setCriadoEm(instante(cadastroHaDias, "08:30"));
        u.setUltimoAcessoEm(instante(Math.min(cadastroHaDias, 2), "20:00"));
        return u;
    }

    private void convite(String startup, String mentor, TipoUsuario enviadoPor, StatusConvite status,
                         int enviadoHaDias, Integer respondidoHaDias) {
        Convite c = new Convite();
        c.setStartupId(startups.get(startup).getId());
        c.setMentorId(mentores.get(mentor).getId());
        c.setEnviadoPor(enviadoPor);
        c.setStatus(status);
        c.setCriadoEm(instante(enviadoHaDias, "11:00"));
        if (respondidoHaDias != null) c.setRespondidoEm(instante(respondidoHaDias, "16:00"));
        conviteRepository.save(c);
    }

    // Mentoria que já aconteceu (confirmada, com horário no passado). O pedido foi feito 5 dias antes.
    private Agendamento realizada(String startup, String mentor, int diasAtras, String hora) {
        return sessao(startup, mentor, agora.toLocalDate().minusDays(diasAtras).atTime(LocalTime.parse(hora)),
                StatusAgendamento.CONFIRMADO, 5);
    }

    // Mentoria daqui a "emDias" dias (negativo = no passado), pedida "pedidoHaDias" dias atrás
    private Agendamento futura(String startup, String mentor, int emDias, String hora, StatusAgendamento status,
                               int pedidoHaDias) {
        Agendamento a = sessao(startup, mentor, agora.toLocalDate().plusDays(emDias).atTime(LocalTime.parse(hora)),
                status, 0);
        a.setCriadoEm(instante(pedidoHaDias, "12:00"));
        if (status == StatusAgendamento.CONFIRMADO) a.setRespondidoEm(a.getCriadoEm().plus(Duration.ofHours(5)));
        return agendamentoRepository.save(a);
    }

    private Agendamento sessao(String startupChave, String mentorChave, LocalDateTime inicio,
                               StatusAgendamento status, int pedidoDiasAntes) {
        Mentor m = mentores.get(mentorChave);
        Agendamento a = new Agendamento();
        a.setStartupId(startups.get(startupChave).getId());
        a.setMentorId(m.getId());
        a.setInicio(inicio);
        a.setFim(inicio.plusMinutes(m.getDuracaoSessaoMinutos() == null ? 60 : m.getDuracaoSessaoMinutos()));
        a.setValor(m.getValorSessao() == null ? BigDecimal.ZERO : m.getValorSessao());
        a.setStatus(status);
        a.setCriadoEm(inicio.minusDays(pedidoDiasAntes).atZone(FUSO).toInstant());
        if (a.getValor().signum() == 0) {
            a.setStatusPagamento(StatusPagamento.GRATUITA);
        } else {
            a.setMetodoPagamento(MetodoPagamento.PIX);
            a.setStatusPagamento(status == StatusAgendamento.RECUSADO ? StatusPagamento.REEMBOLSADO : StatusPagamento.PAGO);
            a.setCodigoTransacao(String.format("PNT-DASH%04d", sequencia++));
            a.setPagoEm(inicio.minusDays(pedidoDiasAntes));
        }
        if (status == StatusAgendamento.CONFIRMADO) {
            a.setLinkReuniao("https://meet.google.com/pnt-" + (500 + sequencia++) + "-dash");
            a.setRespondidoEm(a.getCriadoEm().plus(Duration.ofHours(6)));
        }
        return agendamentoRepository.save(a);
    }

    private void avaliar(Agendamento a, int nota, String comentario) {
        Avaliacao av = new Avaliacao();
        av.setAgendamentoId(a.getId());
        av.setMentorId(a.getMentorId());
        av.setStartupId(a.getStartupId());
        av.setNota(nota);
        av.setComentario(comentario);
        av.setCriadoEm(a.getFim().plusHours(2).atZone(FUSO).toInstant());
        avaliacaoRepository.save(av);
    }

    private Acompanhamento acompanhamento(String mentor, String startup, StatusAcompanhamento status, String objetivo,
                                          int inicioDiasAtras) {
        Acompanhamento a = new Acompanhamento();
        a.setMentorId(mentores.get(mentor).getId());
        a.setStartupId(startups.get(startup).getId());
        a.setObjetivo(objetivo);
        a.setStatus(status);
        a.setDataInicio(agora.toLocalDate().minusDays(inicioDiasAtras));
        a.setCriadoEm(instante(inicioDiasAtras + 1, "10:00"));
        ordemMeta = 0;
        return acompanhamentoRepository.save(a);
    }

    private void meta(Acompanhamento a, String titulo, boolean cumprida) {
        Meta m = new Meta();
        m.setAcompanhamentoId(a.getId());
        m.setTitulo(titulo);
        m.setOrdem(ordemMeta++);
        m.setCumprida(cumprida);
        if (cumprida) m.setCumpridaEm(instante(3, "10:00"));
        metaRepository.save(m);
    }

    // prazoEmDias: negativo = prazo já passou. criadaHaDias: quando o mentor passou a atividade.
    private Atividade atividade(Acompanhamento a, String titulo, int prazoEmDias, StatusAtividade status,
                                int criadaHaDias) {
        Atividade at = new Atividade();
        at.setAcompanhamentoId(a.getId());
        at.setTitulo(titulo);
        at.setPrazo(agora.toLocalDate().plusDays(prazoEmDias).atTime(LocalTime.of(23, 59)));
        at.setStatus(status);
        at.setCriadaEm(instante(criadaHaDias, "10:00"));
        return atividadeRepository.save(at);
    }

    // Entrega enviada "emDias" dias a partir de hoje (negativo = no passado; 0 = hoje)
    private void entrega(Atividade at, int emDias) {
        Entrega e = new Entrega();
        e.setAtividadeId(at.getId());
        e.setTexto("Entrega de demonstração: " + at.getTitulo().toLowerCase());
        LocalDate dia = agora.toLocalDate().plusDays(emDias);
        e.setEnviadaEm(emDias == 0 ? agora.minusMinutes(30) : dia.atTime(18, 0));
        entregaRepository.save(e);
    }
}
