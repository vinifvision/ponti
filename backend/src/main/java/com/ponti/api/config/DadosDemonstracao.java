package com.ponti.api.config;

import com.ponti.api.model.Agendamento;
import com.ponti.api.model.Avaliacao;
import com.ponti.api.model.Certificacao;
import com.ponti.api.model.Convite;
import com.ponti.api.model.Disponibilidade;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.MetodoPagamento;
import com.ponti.api.model.StatusAgendamento;
import com.ponti.api.model.StatusConvite;
import com.ponti.api.model.StatusPagamento;
import com.ponti.api.model.Startup;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.AvaliacaoRepository;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.ConviteRepository;
import com.ponti.api.repository.DisponibilidadeRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Dados de DEMONSTRAÇÃO: deixam a plataforma com cara de "em uso" (mais mentores e startups,
// mentorias passadas com avaliações, pedidos esperando resposta e sessões confirmadas).
// Roda depois de DadosIniciais e só com o banco ainda sem esses perfis.
// Desligue com ponti.demo.completo=false (os testes automáticos desligam, para trabalhar com poucos dados).
//
// Logins extras (senha ponti1234): camila@, rafael@, beatriz@, thiago@, fernanda@, marcos@, anapaula@,
// diego@, patricia@, lucas@ (mentores) e agrosense@, edutrilha@, finfacil@, mareslimpas@ (startups), todos @exemplo.com
@Component
@Order(2)
@ConditionalOnProperty(name = "ponti.demo.completo", havingValue = "true", matchIfMissing = true)
public class DadosDemonstracao implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracao.class);

    private final MentorRepository mentorRepository;
    private final StartupRepository startupRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final CertificacaoRepository certificacaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final ConviteRepository conviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock relogio;

    private final Map<String, Mentor> mentores = new HashMap<>();
    private final Map<String, Startup> startups = new HashMap<>();
    private LocalDate hoje;
    private int sequencia = 1;

    public DadosDemonstracao(MentorRepository mentorRepository, StartupRepository startupRepository,
                             UsuarioRepository usuarioRepository, DisponibilidadeRepository disponibilidadeRepository,
                             CertificacaoRepository certificacaoRepository, AgendamentoRepository agendamentoRepository,
                             AvaliacaoRepository avaliacaoRepository, ConviteRepository conviteRepository,
                             PasswordEncoder passwordEncoder, Clock relogio) {
        this.mentorRepository = mentorRepository;
        this.startupRepository = startupRepository;
        this.usuarioRepository = usuarioRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.certificacaoRepository = certificacaoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.conviteRepository = conviteRepository;
        this.passwordEncoder = passwordEncoder;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.findByEmail("camila@exemplo.com").isPresent()) {
            return; // já carregado (ex.: banco persistente)
        }
        hoje = LocalDate.now(relogio);
        carregarPerfisBase();
        criarMentores();
        criarStartups();
        criarHistorico();
        log.info("Dados de demonstração carregados: {} mentores, {} startups, {} mentorias",
                mentorRepository.count(), startupRepository.count(), agendamentoRepository.count());
    }

    // ---------------------------------------------------------------- perfis

    private void carregarPerfisBase() {
        usuarioRepository.findByEmail("juliana@exemplo.com").ifPresent(u -> mentores.put("juliana", mentorRepository.findById(u.getMentorId()).orElseThrow()));
        usuarioRepository.findByEmail("roberto@exemplo.com").ifPresent(u -> mentores.put("roberto", mentorRepository.findById(u.getMentorId()).orElseThrow()));
        usuarioRepository.findByEmail("petcare@exemplo.com").ifPresent(u -> startups.put("petcare", startupRepository.findById(u.getStartupId()).orElseThrow()));
        usuarioRepository.findByEmail("saudeagora@exemplo.com").ifPresent(u -> startups.put("saudeagora", startupRepository.findById(u.getStartupId()).orElseThrow()));
    }

    private void criarMentores() {
        mentor("camila", "Camila Andrade",
                "Gerente de produto há 11 anos, passei por fintechs e SaaS B2B. Ajudo fundadores a sair da ideia para um MVP que resolve uma dor real, com entrevistas de descoberta e métricas desde o primeiro dia.",
                "Produto, UX Research", "Fintechs, SaaS", 11, "180.00", 60, "https://www.linkedin.com/in/exemplo-camila");
        disponivel("camila", DayOfWeek.MONDAY, "09:00", "12:00");
        disponivel("camila", DayOfWeek.WEDNESDAY, "19:00", "21:00");
        certificacao("camila", "Product Management Certificate", "Product School", 2019, 2019, "Formação em gestão de produto digital.");
        certificacao("camila", "Mentora do programa de aceleração", "Porto Digital", 2023, 2026, "Acompanhamento de startups em fase de validação.");

        mentor("rafael", "Rafael Monteiro",
                "Investidor-anjo e ex-fundador (exit em 2019). Já participei de mais de 30 rodadas pré-seed e seed. Mentoria focada em pitch, valuation e como chegar no investidor certo.",
                "Investimento, Captação", "Fintechs, Marketplaces", 15, "250.00", 45, "https://www.linkedin.com/in/exemplo-rafael");
        disponivel("rafael", DayOfWeek.TUESDAY, "18:00", "21:00");
        disponivel("rafael", DayOfWeek.THURSDAY, "18:00", "21:00");
        certificacao("rafael", "Membro de rede de investidores-anjo", "Anjos do Brasil", 2018, 2026, "Análise e coinvestimento em startups early stage.");

        mentor("beatriz", "Beatriz Lins",
                "Especialista em growth e marketing digital. Montei o time de aquisição de uma edtech que cresceu 10x em dois anos. Vamos achar o seu canal de aquisição mais barato.",
                "Marketing digital, Growth", "Edtechs, E-commerce", 7, "90.00", 45, "https://www.linkedin.com/in/exemplo-beatriz");
        disponivel("beatriz", DayOfWeek.MONDAY, "14:00", "18:00");
        disponivel("beatriz", DayOfWeek.FRIDAY, "14:00", "17:00");
        certificacao("beatriz", "Growth Marketing", "Reforge", 2022, 2022, "Programa de growth, retenção e monetização.");

        mentor("thiago", "Thiago Cavalcanti",
                "CTO de healthtech e engenheiro de software há 12 anos. Ajudo a escolher a stack, planejar a arquitetura do MVP e montar o primeiro time técnico sem desperdício.",
                "Tecnologia, Arquitetura de software", "SaaS, Health-techs", 12, "150.00", 60, "https://www.linkedin.com/in/exemplo-thiago");
        disponivel("thiago", DayOfWeek.TUESDAY, "08:00", "10:00");
        disponivel("thiago", DayOfWeek.WEDNESDAY, "14:00", "17:00");
        disponivel("thiago", DayOfWeek.FRIDAY, "08:00", "10:00");
        certificacao("thiago", "AWS Certified Solutions Architect", "Amazon Web Services", 2021, 2024, "Arquitetura de soluções em nuvem.");
        certificacao("thiago", "Mestrado em Ciência da Computação", "UFPE", 2013, 2015, "Engenharia de software.");

        mentor("fernanda", "Fernanda Rocha",
                "Advogada especializada em startups: contrato social, acordo de sócios, vesting, LGPD e termos de uso. Sessões curtas e objetivas para você não travar por causa do jurídico.",
                "Jurídico, LGPD", "Todas as áreas", 9, "200.00", 30, "https://www.linkedin.com/in/exemplo-fernanda");
        disponivel("fernanda", DayOfWeek.MONDAY, "10:00", "12:00");
        disponivel("fernanda", DayOfWeek.THURSDAY, "10:00", "12:00");
        certificacao("fernanda", "Especialização em Direito Digital", "FGV", 2018, 2019, "Proteção de dados e contratos de tecnologia.");

        mentor("marcos", "Marcos Vieira",
                "18 anos em vendas B2B. Ajudo a estruturar o processo comercial, definir o cliente ideal e fechar os primeiros contratos, mesmo sem time de vendas.",
                "Vendas B2B, Comercial", "SaaS, Agritechs", 18, "160.00", 60, "https://www.linkedin.com/in/exemplo-marcos");
        disponivel("marcos", DayOfWeek.WEDNESDAY, "09:00", "12:00");
        disponivel("marcos", DayOfWeek.FRIDAY, "09:00", "12:00");

        mentor("anapaula", "Ana Paula Souza",
                "Engenheira agrônoma e pesquisadora. Conecto startups do agro com produtores para testar soluções em campo. Mentoria gratuita para projetos de impacto no semiárido.",
                "Agronegócio, Sustentabilidade", "Agritechs, Impacto social", 14, "0", 45, "https://www.linkedin.com/in/exemplo-anapaula");
        mentores.get("anapaula").setFotoUrl("/demo/mentores/ana-paula.svg");
        mentorRepository.save(mentores.get("anapaula"));
        disponivel("anapaula", DayOfWeek.TUESDAY, "14:00", "17:00");
        disponivel("anapaula", DayOfWeek.THURSDAY, "14:00", "17:00");
        certificacao("anapaula", "Doutorado em Agronomia", "UFRPE", 2012, 2016, "Manejo de irrigação no semiárido.");

        mentor("diego", "Diego Albuquerque",
                "Pedagogo e designer instrucional. Ajudo edtechs a desenhar trilhas de aprendizagem que engajam e a medir se o aluno realmente aprendeu.",
                "Educação, Design instrucional", "Edtechs", 6, "80.00", 30, "https://www.linkedin.com/in/exemplo-diego");
        disponivel("diego", DayOfWeek.MONDAY, "19:00", "21:00");
        disponivel("diego", DayOfWeek.WEDNESDAY, "19:00", "21:00");

        mentor("patricia", "Patrícia Nunes",
                "16 anos em assuntos regulatórios de saúde. Oriento healthtechs e pettechs sobre ANVISA, CFM, CFMV e LGPD em dados de saúde, antes que isso vire um problema.",
                "Regulatório em saúde", "Health-techs, PetTech", 16, "220.00", 60, "https://www.linkedin.com/in/exemplo-patricia");
        disponivel("patricia", DayOfWeek.TUESDAY, "10:00", "12:00");
        disponivel("patricia", DayOfWeek.FRIDAY, "14:00", "16:00");
        certificacao("patricia", "MBA em Gestão de Saúde", "Fiocruz", 2014, 2015, "Regulação e políticas de saúde.");

        mentor("lucas", "Lucas Ferraz",
                "Facilitador de Lean Startup e Business Model Canvas. Já mentorei mais de 80 startups em programas de pré-aceleração. Primeira conversa é gratuita.",
                "Modelo de negócio, Lean Startup", "Todas as áreas, Impacto social", 10, "0", 60, "https://www.linkedin.com/in/exemplo-lucas");
        disponivel("lucas", DayOfWeek.MONDAY, "08:00", "10:00");
        disponivel("lucas", DayOfWeek.THURSDAY, "16:00", "19:00");
        certificacao("lucas", "Mentor de pré-aceleração", "Sebrae PE", 2020, 2026, "Programas de validação de modelo de negócio.");
    }

    private void criarStartups() {
        startup("agrosense", "AgroSense",
                "Sensores de umidade de solo de baixo custo com alertas por WhatsApp para pequenos produtores do Vale do São Francisco.",
                "Agritech", "Petrolina", "PE", 3);
        startup("edutrilha", "EduTrilha",
                "Plataforma de trilhas de estudo para o ENEM com conteúdo adaptado ao ritmo de cada aluno da rede pública.",
                "Edtech", "Caruaru", "PE", 2);
        startup("finfacil", "FinFácil",
                "Controle financeiro simples para MEIs, com emissão de nota e lembrete do DAS pelo celular.",
                "Fintech", "Recife", "PE", 4);
        startup("mareslimpas", "Marés Limpas",
                "Coleta e rastreio de resíduos plásticos nas praias, com pagamento a catadores por quilo recolhido.",
                "Impacto social", "Recife", "PE", 1);
        startups.get("mareslimpas").setFotoUrl("/demo/startups/mareslimpas.svg");
        startupRepository.save(startups.get("mareslimpas"));
    }

    // ---------------------------------------------------------------- histórico

    private void criarHistorico() {
        // PetCare (conta de demonstração da startup): já fez mentorias, tem uma confirmada, um pedido pendente,
        // uma mentoria realizada esperando avaliação e uma sugestão de mentor
        Agendamento a;
        a = passada("petcare", "juliana", 24, "09:00", MetodoPagamento.PIX, "Validar se os tutores pagariam por teleconsulta.");
        avaliar(a, 5, "A Juliana trouxe um roteiro de entrevistas com tutores que mudou nossa proposta de valor. Muito objetiva!");
        a = passada("petcare", "camila", 17, "10:00", MetodoPagamento.CARTAO, "Priorizar as funcionalidades do MVP.");
        avaliar(a, 4, "Ótima para priorizar o MVP. Saímos com um backlog bem mais enxuto.");
        passada("petcare", "patricia", 6, "10:00", MetodoPagamento.PIX, "Entender o que o CFMV exige para teleorientação veterinária.");
        futura("petcare", "thiago", DayOfWeek.WEDNESDAY, "14:00", StatusAgendamento.CONFIRMADO, MetodoPagamento.CARTAO,
                "Revisar a arquitetura do app antes de contratar o primeiro dev.");
        futura("petcare", "rafael", DayOfWeek.THURSDAY, "18:00", StatusAgendamento.PENDENTE, MetodoPagamento.PIX,
                "Preparar o pitch para a primeira rodada anjo.");
        sugestao("lucas", "petcare");

        // Juliana (conta de demonstração do mentor): pedidos para confirmar, próximas sessões e histórico avaliado
        futura("saudeagora", "juliana", DayOfWeek.TUESDAY, "10:00", StatusAgendamento.PENDENTE, MetodoPagamento.PIX,
                "Queremos validar o fluxo de triagem com recepcionistas de clínicas.");
        futura("edutrilha", "juliana", DayOfWeek.THURSDAY, "14:00", StatusAgendamento.PENDENTE, MetodoPagamento.CARTAO,
                "Como fazer entrevistas de descoberta com alunos e professores?");
        futura("finfacil", "juliana", DayOfWeek.THURSDAY, "10:00", StatusAgendamento.CONFIRMADO, MetodoPagamento.CARTAO,
                "Validação do onboarding com MEIs.");
        a = passada("saudeagora", "juliana", 31, "10:00", MetodoPagamento.CARTAO, null);
        avaliar(a, 5, "Super didática. Nos ajudou a definir as hipóteses certas antes de programar.");
        a = passada("mareslimpas", "juliana", 12, "11:00", MetodoPagamento.PIX, null);
        avaliar(a, 4, "Bons exemplos de validação, mesmo não sendo da nossa área.");
        passada("agrosense", "juliana", 40, "09:00", MetodoPagamento.PIX, null);

        // Reputação dos demais mentores
        avaliar(passada("finfacil", "rafael", 20, "19:00", MetodoPagamento.CARTAO, null), 5, "Revisou nosso pitch slide a slide. Saímos prontos para a banca.");
        avaliar(passada("agrosense", "rafael", 45, "18:00", MetodoPagamento.PIX, null), 5, "Direto ao ponto sobre valuation e diluição.");
        avaliar(passada("saudeagora", "rafael", 60, "20:00", MetodoPagamento.CARTAO, null), 4, null);
        avaliar(passada("edutrilha", "beatriz", 9, "15:00", MetodoPagamento.PIX, null), 5, "Saímos com 3 experimentos de aquisição para rodar na semana.");
        avaliar(passada("finfacil", "beatriz", 33, "14:00", MetodoPagamento.PIX, null), 4, "Muitas ideias práticas de conteúdo para MEIs.");
        avaliar(passada("finfacil", "thiago", 14, "08:00", MetodoPagamento.CARTAO, null), 5, "Ajudou a evitar uma arquitetura complexa demais para o MVP.");
        avaliar(passada("saudeagora", "thiago", 26, "15:00", MetodoPagamento.PIX, null), 5, "Excelente! Recomendo para quem está escolhendo a stack.");
        avaliar(passada("edutrilha", "thiago", 50, "09:00", MetodoPagamento.PIX, null), 4, null);
        avaliar(passada("finfacil", "fernanda", 8, "10:00", MetodoPagamento.CARTAO, null), 5, "Resolveu nosso acordo de sócios em 30 minutos.");
        avaliar(passada("agrosense", "fernanda", 38, "11:00", MetodoPagamento.PIX, null), 5, "Muito clara sobre LGPD para dados dos produtores.");
        avaliar(passada("agrosense", "marcos", 16, "10:00", MetodoPagamento.PIX, null), 5, "Montamos o funil de vendas para cooperativas.");
        avaliar(passada("finfacil", "marcos", 44, "09:00", MetodoPagamento.CARTAO, null), 4, "Bom para quem nunca vendeu B2B.");
        avaliar(passada("agrosense", "anapaula", 11, "15:00", null, null), 5, "Conseguiu produtores para testarmos os sensores em campo!");
        avaliar(passada("mareslimpas", "anapaula", 29, "14:00", null, null), 5, "Generosa e cheia de contatos.");
        avaliar(passada("edutrilha", "diego", 13, "19:00", MetodoPagamento.PIX, null), 5, "Redesenhamos a trilha de matemática depois da conversa.");
        avaliar(passada("edutrilha", "diego", 41, "20:00", MetodoPagamento.PIX, null), 4, null);
        avaliar(passada("saudeagora", "patricia", 19, "10:00", MetodoPagamento.CARTAO, null), 5, "Esclareceu tudo sobre o que precisa ou não de registro na ANVISA.");
        avaliar(passada("mareslimpas", "lucas", 22, "08:00", null, null), 5, "Canvas preenchido e hipóteses priorizadas. Ótimo começo.");
        avaliar(passada("edutrilha", "lucas", 52, "17:00", null, null), 4, "Boa dinâmica, recomendo para quem está começando.");
        avaliar(passada("petcare", "roberto", 35, "14:00", null, null), 5, "Conhece muito o mercado pet. Indicou clínicas parceiras.");
        avaliar(passada("saudeagora", "roberto", 48, "15:00", null, null), 4, null);
    }

    // ---------------------------------------------------------------- apoio

    private void mentor(String chave, String nome, String bio, String areas, String ecossistemas,
                        int anos, String valor, int duracao, String linkedin) {
        Mentor m = new Mentor();
        m.setNome(nome);
        m.setBio(bio);
        m.setAreasAtuacao(areas);
        m.setEcossistemas(ecossistemas);
        m.setAnosExperiencia(anos);
        m.setValorSessao(new BigDecimal(valor));
        m.setDuracaoSessaoMinutos(duracao);
        m.setLinkedinUrl(linkedin);
        m.setFotoUrl("/demo/mentores/" + chave + ".svg");
        m = mentorRepository.save(m);
        mentores.put(chave, m);
        Usuario u = usuario(chave + "@exemplo.com", TipoUsuario.MENTOR);
        u.setMentorId(m.getId());
        usuarioRepository.save(u);
    }

    private void startup(String chave, String nome, String descricao, String segmento,
                         String cidade, String estado, int estagio) {
        Startup s = new Startup();
        s.setNome(nome);
        s.setDescricao(descricao);
        s.setSegmento(segmento);
        s.setCidade(cidade);
        s.setEstado(estado);
        s.setEstagioMaturidade(estagio);
        s.setFotoUrl("/demo/startups/" + chave + ".svg");
        s = startupRepository.save(s);
        startups.put(chave, s);
        Usuario u = usuario(chave + "@exemplo.com", TipoUsuario.STARTUP);
        u.setStartupId(s.getId());
        usuarioRepository.save(u);
    }

    private Usuario usuario(String email, TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setSenhaHash(passwordEncoder.encode(DadosIniciais.SENHA_EXEMPLO));
        u.setTipo(tipo);
        return u;
    }

    private void disponivel(String mentor, DayOfWeek dia, String inicio, String fim) {
        Disponibilidade d = new Disponibilidade();
        d.setMentorId(mentores.get(mentor).getId());
        d.setDiaSemana(dia);
        d.setHoraInicio(LocalTime.parse(inicio));
        d.setHoraFim(LocalTime.parse(fim));
        disponibilidadeRepository.save(d);
    }

    private void certificacao(String mentor, String titulo, String instituicao, int de, int ate, String descricao) {
        Certificacao c = new Certificacao();
        c.setMentorId(mentores.get(mentor).getId());
        c.setTitulo(titulo);
        c.setInstituicao(instituicao);
        c.setAnoInicio(de);
        c.setAnoFim(ate);
        c.setDescricao(descricao);
        certificacaoRepository.save(c);
    }

    // Mentoria que já aconteceu ("realizada": confirmada e com horário no passado)
    private Agendamento passada(String startup, String mentor, int diasAtras, String hora,
                                MetodoPagamento metodo, String mensagem) {
        LocalDateTime inicio = hoje.minusDays(diasAtras).atTime(LocalTime.parse(hora));
        return sessao(startup, mentor, inicio, StatusAgendamento.CONFIRMADO, metodo, mensagem);
    }

    // Mentoria futura, no próximo dia da semana indicado (pelo menos 2 dias à frente, dentro da agenda do mentor)
    private Agendamento futura(String startup, String mentor, DayOfWeek dia, String hora,
                               StatusAgendamento status, MetodoPagamento metodo, String mensagem) {
        LocalDate data = hoje.plusDays(2).with(TemporalAdjusters.nextOrSame(dia));
        return sessao(startup, mentor, data.atTime(LocalTime.parse(hora)), status, metodo, mensagem);
    }

    private Agendamento sessao(String startupChave, String mentorChave, LocalDateTime inicio,
                               StatusAgendamento status, MetodoPagamento metodo, String mensagem) {
        Mentor m = mentores.get(mentorChave);
        Startup s = startups.get(startupChave);
        Agendamento a = new Agendamento();
        a.setStartupId(s.getId());
        a.setMentorId(m.getId());
        a.setInicio(inicio);
        a.setFim(inicio.plusMinutes(m.getDuracaoSessaoMinutos() == null ? 60 : m.getDuracaoSessaoMinutos()));
        a.setValor(m.getValorSessao() == null ? BigDecimal.ZERO : m.getValorSessao());
        a.setStatus(status);
        a.setMensagem(mensagem);
        a.setCriadoEm(inicio.minusDays(5).atZone(ZoneId.of("America/Recife")).toInstant());
        if (a.getValor().signum() == 0 || metodo == null) {
            a.setStatusPagamento(StatusPagamento.GRATUITA);
        } else {
            a.setMetodoPagamento(metodo);
            a.setStatusPagamento(StatusPagamento.PAGO);
            a.setCodigoTransacao(String.format("PNT-DEMO%04d", sequencia++));
            a.setPagoEm(inicio.minusDays(5));
            if (metodo == MetodoPagamento.CARTAO) {
                a.setCartaoBandeira(sequencia % 2 == 0 ? "Visa" : "Mastercard");
                a.setCartaoFinal(sequencia % 2 == 0 ? "4242" : "5454");
            }
        }
        if (status == StatusAgendamento.CONFIRMADO) {
            a.setLinkReuniao("https://meet.google.com/pnt-" + (100 + sequencia) + "-demo");
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
        av.setCriadoEm(a.getFim().plusHours(2).atZone(ZoneId.of("America/Recife")).toInstant());
        avaliacaoRepository.save(av);
    }

    private void sugestao(String mentor, String startup) {
        Convite c = new Convite();
        c.setMentorId(mentores.get(mentor).getId());
        c.setStartupId(startups.get(startup).getId());
        c.setEnviadoPor(TipoUsuario.MENTOR);
        c.setStatus(StatusConvite.PENDENTE);
        conviteRepository.save(c);
    }
}
