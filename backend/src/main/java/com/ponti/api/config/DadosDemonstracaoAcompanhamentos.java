package com.ponti.api.config;

import com.ponti.api.model.Acompanhamento;
import com.ponti.api.model.Atividade;
import com.ponti.api.model.Comentario;
import com.ponti.api.model.Entrega;
import com.ponti.api.model.Meta;
import com.ponti.api.model.StatusAcompanhamento;
import com.ponti.api.model.StatusAtividade;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.AcompanhamentoRepository;
import com.ponti.api.repository.AtividadeRepository;
import com.ponti.api.repository.ComentarioRepository;
import com.ponti.api.repository.EntregaRepository;
import com.ponti.api.repository.MetaRepository;
import com.ponti.api.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Acompanhamentos de DEMONSTRAÇÃO, para a aba não aparecer vazia no pitch.
// Ficam nas contas de demonstração (senha ponti1234):
//  - petcare@exemplo.com (startup): um ativo com a Juliana (atividades em todos os estados), uma proposta
//    da Patrícia para aceitar ao vivo e um encerrado com a Camila;
//  - juliana@exemplo.com (mentora): o ativo com a PetCare, um ativo com a Marés Limpas (uma entrega para
//    revisar) e uma proposta esperando a SaúdeAgora. A AgroSense fica livre para propor ao vivo.
// Só roda se ainda não existe nenhum acompanhamento. Por isso funciona também no banco do Supabase,
// que já tinha os outros dados de demonstração. As datas são relativas a hoje.
@Component
@Order(3)
@ConditionalOnProperty(name = "ponti.demo.completo", havingValue = "true", matchIfMissing = true)
public class DadosDemonstracaoAcompanhamentos implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracaoAcompanhamentos.class);

    private final UsuarioRepository usuarioRepository;
    private final AcompanhamentoRepository acompanhamentoRepository;
    private final MetaRepository metaRepository;
    private final AtividadeRepository atividadeRepository;
    private final EntregaRepository entregaRepository;
    private final ComentarioRepository comentarioRepository;
    private final Clock relogio;

    private LocalDateTime agora;
    private int ordemMeta;

    public DadosDemonstracaoAcompanhamentos(UsuarioRepository usuarioRepository,
                                            AcompanhamentoRepository acompanhamentoRepository,
                                            MetaRepository metaRepository,
                                            AtividadeRepository atividadeRepository,
                                            EntregaRepository entregaRepository,
                                            ComentarioRepository comentarioRepository,
                                            Clock relogio) {
        this.usuarioRepository = usuarioRepository;
        this.acompanhamentoRepository = acompanhamentoRepository;
        this.metaRepository = metaRepository;
        this.atividadeRepository = atividadeRepository;
        this.entregaRepository = entregaRepository;
        this.comentarioRepository = comentarioRepository;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (acompanhamentoRepository.count() > 0) {
            return; // já existe (dados de demonstração ou de uso real): não mexe
        }
        Long petcare = startup("petcare@exemplo.com");
        Long saudeagora = startup("saudeagora@exemplo.com");
        Long mareslimpas = startup("mareslimpas@exemplo.com");
        Long juliana = mentor("juliana@exemplo.com");
        Long camila = mentor("camila@exemplo.com");
        Long patricia = mentor("patricia@exemplo.com");
        if (petcare == null || juliana == null || camila == null || patricia == null
                || saudeagora == null || mareslimpas == null) {
            return; // contas de demonstração não existem neste banco
        }
        agora = LocalDateTime.now(relogio).withSecond(0).withNano(0);

        petcareComCamilaEncerrado(petcare, camila);
        julianaComMaresLimpas(juliana, mareslimpas);
        petcareComJuliana(petcare, juliana);
        propostas(petcare, patricia, saudeagora, juliana);

        log.info("Acompanhamentos de demonstração carregados: {}", acompanhamentoRepository.count());
    }

    // ---------------------------------------------------------------- acompanhamentos

    // O principal: mostra todos os estados de atividade (aprovada, aguardando revisão, devolvida, atrasada e pendente)
    private void petcareComJuliana(Long petcare, Long juliana) {
        Acompanhamento a = acompanhamento(juliana, petcare, StatusAcompanhamento.ATIVO,
                "Validar a teleconsulta veterinária e fechar os 3 primeiros clientes piloto.", 21, null);
        meta(a, "Entrevistar 10 tutores de pets", true);
        meta(a, "Definir a proposta de valor da teleconsulta", true);
        meta(a, "Validar o preço com 3 clínicas parceiras", false);
        meta(a, "Fechar 3 clientes piloto", false);

        Atividade at = atividade(a, "Mapear e agendar 10 entrevistas com tutores",
                "Liste tutores de cães e gatos da sua região e agende conversas de 30 minutos para entender como eles cuidam da saúde do pet hoje.",
                -14, StatusAtividade.APROVADA);
        Entrega e = entrega(at, "Listei 14 tutores e já agendei 10 conversas. A planilha tem o perfil de cada um.",
                "https://docs.google.com/spreadsheets/d/exemplo-entrevistas", dias(-15, "10:20"),
                "Ótima lista! Priorize quem já usou algum serviço online para o pet.");
        comentario(e, TipoUsuario.MENTOR, "Podemos revisar o roteiro de perguntas na próxima reunião?", dias(-14, "09:00"));
        comentario(e, TipoUsuario.STARTUP, "Pode ser! Levo as primeiras perguntas prontas.", dias(-14, "11:40"));

        at = atividade(a, "Consolidar os aprendizados das entrevistas",
                "Resuma os padrões que apareceram nas conversas: dores mais citadas, como os tutores resolvem hoje e as objeções.",
                -5, StatusAtividade.ENTREGUE);
        e = entrega(at, "Resumi os padrões e as 3 dores mais citadas, com trechos das falas dos tutores.",
                "https://www.notion.so/exemplo-aprendizados", dias(-6, "15:30"), null);
        comentario(e, TipoUsuario.STARTUP, "Se preferir, apresento rapidinho na nossa próxima conversa.", dias(-6, "15:45"));

        at = atividade(a, "Desenhar o mapa da proposta de valor",
                "Monte o mapa com dores, ganhos e alívios para cada perfil de tutor que você identificou.",
                -2, StatusAtividade.DEVOLVIDA);
        entrega(at, "Primeira versão do mapa, ainda com um perfil só de tutor.", null, dias(-3, "18:05"),
                "Faltou separar os ganhos por perfil de tutor (cão, gato e pets exóticos). Revise e reenvie.");

        atividade(a, "Escrever as hipóteses a validar",
                "Transforme o que aprendeu em hipóteses testáveis, cada uma com o critério que mostra se ela estava certa.",
                -1, StatusAtividade.PENDENTE);

        atividade(a, "Apresentar a proposta a 3 clínicas parceiras",
                "Mostre a proposta de teleconsulta a pelo menos 3 clínicas e anote o que elas mudariam no serviço.",
                7, StatusAtividade.PENDENTE);
    }

    private void julianaComMaresLimpas(Long juliana, Long mareslimpas) {
        Acompanhamento a = acompanhamento(juliana, mareslimpas, StatusAcompanhamento.ATIVO,
                "Validar o modelo de pagamento por quilo com os catadores.", 10, null);
        meta(a, "Entrevistar 8 catadores", true);
        meta(a, "Definir o valor pago por quilo", false);
        meta(a, "Rodar um piloto de 2 semanas", false);

        Atividade at = atividade(a, "Entrevistar 8 catadores das praias de Boa Viagem e do Pina",
                "Entenda quanto eles recolhem por dia, como vendem hoje e o que os faria trocar de comprador.",
                -6, StatusAtividade.APROVADA);
        entrega(at, "Conversei com 9 catadores. A maioria vende para atravessadores e recebe só no fim do mês.",
                null, dias(-7, "17:10"), "Excelente achado: receber na hora pode ser o seu diferencial.");

        at = atividade(a, "Calcular o custo por quilo recolhido",
                "Some transporte, triagem e o valor pago ao catador para chegar ao custo de cada quilo.",
                -1, StatusAtividade.ENTREGUE);
        entrega(at, "Cheguei a R$ 1,80 por quilo. A planilha tem os três cenários de volume.",
                "https://docs.google.com/spreadsheets/d/exemplo-custos", dias(-1, "09:15"), null);

        atividade(a, "Testar o pagamento por Pix com 5 catadores",
                "Pague 5 catadores por Pix logo na entrega do material durante uma semana e anote a reação deles.",
                6, StatusAtividade.PENDENTE);
    }

    private void petcareComCamilaEncerrado(Long petcare, Long camila) {
        Acompanhamento a = acompanhamento(camila, petcare, StatusAcompanhamento.ENCERRADO,
                "Priorizar o backlog e definir o escopo do MVP.", 16, 2);
        meta(a, "Backlog priorizado", true);
        meta(a, "Escopo do MVP fechado", true);
        String[][] feitas = {
                {"Listar todas as ideias de funcionalidade", "Junte tudo o que já foi pedido ou pensado, sem filtrar."},
                {"Priorizar com a matriz de impacto e esforço", "Classifique cada item e escolha os 5 primeiros."},
                {"Escrever o escopo do MVP em uma página", "Descreva o que entra e, principalmente, o que fica de fora."},
        };
        int[] prazos = {-12, -8, -4};
        for (int i = 0; i < feitas.length; i++) {
            Atividade at = atividade(a, feitas[i][0], feitas[i][1], prazos[i], StatusAtividade.APROVADA);
            entrega(at, "Entrega concluída.", null, dias(prazos[i] - 1, "16:00"), i == 2 ? "Escopo enxuto e claro. Parabéns!" : null);
        }
    }

    private void propostas(Long petcare, Long patricia, Long saudeagora, Long juliana) {
        // Para a PetCare aceitar ou recusar ao vivo
        Acompanhamento a = acompanhamento(patricia, petcare, StatusAcompanhamento.PROPOSTO,
                "Organizar o regulatório da teleorientação veterinária (CFMV e LGPD).", 0, null);
        meta(a, "Mapear as exigências do CFMV para teleorientação", false);
        meta(a, "Escrever a política de privacidade dos dados de tutores e pets", false);

        // Para a Juliana ver uma proposta esperando resposta
        a = acompanhamento(juliana, saudeagora, StatusAcompanhamento.PROPOSTO,
                "Estruturar o piloto com a primeira clínica parceira.", 0, null);
        meta(a, "Definir os indicadores do piloto", false);
        meta(a, "Assinar o termo de piloto com a clínica", false);
    }

    // ---------------------------------------------------------------- apoio

    private Long startup(String email) {
        return usuarioRepository.findByEmail(email).map(Usuario::getStartupId).orElse(null);
    }

    private Long mentor(String email) {
        return usuarioRepository.findByEmail(email).map(Usuario::getMentorId).orElse(null);
    }

    private LocalDateTime dias(int dias, String hora) {
        return agora.toLocalDate().plusDays(dias).atTime(LocalTime.parse(hora));
    }

    private Acompanhamento acompanhamento(Long mentor, Long startup, StatusAcompanhamento status, String objetivo,
                                          int inicioDiasAtras, Integer fimDiasAtras) {
        Acompanhamento a = new Acompanhamento();
        a.setMentorId(mentor);
        a.setStartupId(startup);
        a.setObjetivo(objetivo);
        a.setStatus(status);
        if (status != StatusAcompanhamento.PROPOSTO) a.setDataInicio(agora.toLocalDate().minusDays(inicioDiasAtras));
        if (fimDiasAtras != null) a.setDataFim(agora.toLocalDate().minusDays(fimDiasAtras));
        a.setCriadoEm(agora.minusDays(inicioDiasAtras + 1L).atZone(ZoneId.of("America/Recife")).toInstant());
        ordemMeta = 0;
        return acompanhamentoRepository.save(a);
    }

    private void meta(Acompanhamento a, String titulo, boolean cumprida) {
        Meta m = new Meta();
        m.setAcompanhamentoId(a.getId());
        m.setTitulo(titulo);
        m.setOrdem(ordemMeta++);
        m.setCumprida(cumprida);
        if (cumprida) m.setCumpridaEm(relogio.instant());
        metaRepository.save(m);
    }

    private Atividade atividade(Acompanhamento a, String titulo, String descricao, int prazoEmDias, StatusAtividade status) {
        Atividade at = new Atividade();
        at.setAcompanhamentoId(a.getId());
        at.setTitulo(titulo);
        at.setDescricao(descricao);
        at.setPrazo(dias(prazoEmDias, "23:59"));
        at.setStatus(status);
        return atividadeRepository.save(at);
    }

    private Entrega entrega(Atividade at, String texto, String link, LocalDateTime enviadaEm, String feedback) {
        Entrega e = new Entrega();
        e.setAtividadeId(at.getId());
        e.setTexto(texto);
        e.setLink(link);
        e.setEnviadaEm(enviadaEm);
        e.setFeedback(feedback);
        return entregaRepository.save(e);
    }

    private void comentario(Entrega e, TipoUsuario autor, String texto, LocalDateTime quando) {
        Comentario c = new Comentario();
        c.setEntregaId(e.getId());
        c.setAutor(autor);
        c.setTexto(texto);
        c.setCriadoEm(quando);
        comentarioRepository.save(c);
    }
}
