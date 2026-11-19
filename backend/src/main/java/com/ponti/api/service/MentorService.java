package com.ponti.api.service;

import com.ponti.api.dto.AtualizacaoMentor;
import com.ponti.api.dto.MentorResposta;
import com.ponti.api.dto.Pagina;
import com.ponti.api.model.Mentor;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.AvaliacaoRepository;
import com.ponti.api.repository.MentorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Consulta e edição do perfil do mentor. As respostas já trazem a reputação
// (média das avaliações e quantidade de mentorias realizadas), calculada com 2 consultas agrupadas.
@Service
public class MentorService {

    private final MentorRepository mentorRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final IndicadoresMentorService indicadores;
    private final Clock relogio;

    public MentorService(MentorRepository mentorRepository, AvaliacaoRepository avaliacaoRepository,
                         AgendamentoRepository agendamentoRepository, IndicadoresMentorService indicadores,
                         Clock relogio) {
        this.mentorRepository = mentorRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.indicadores = indicadores;
        this.relogio = relogio;
    }

    public List<MentorResposta> listar() {
        Reputacao r = reputacao();
        return mentorRepository.findAll().stream().map(r::aplicar).toList();
    }

    // Busca geral (GET /mentores, para quem não é startup): mesmos filtros e paginação da busca da startup,
    // em ordem alfabética. Sem match não existe ordem de recomendação.
    public Pagina<MentorResposta> pesquisar(BuscaMentores.Filtro filtro) {
        if (filtro.ordenacao() == BuscaMentores.Ordenacao.RECOMENDADOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A ordenação recomendados é só para startups, em /mentores/recomendados");
        }
        Set<Long> comHorario = indicadores.comHorarioNaSemana();
        List<MentorResposta> porNome = new ArrayList<>(listar());
        porNome.sort(BuscaMentores.porCampo(BuscaMentores.Ordenacao.NOME, (MentorResposta m) -> m)
                .thenComparing(MentorResposta::id));
        return BuscaMentores.buscar(porNome, filtro, m -> filtro.aceita(m, comHorario.contains(m.id())), m -> m);
    }

    // Todas as áreas que os mentores atendem (opções do filtro "Área de atuação")
    public List<String> areas() {
        return BuscaMentores.areas(mentorRepository.findAll().stream().map(Mentor::getEcossistemas).toList());
    }

    public MentorResposta buscar(Long id) {
        return reputacao().aplicar(buscarEntidade(id));
    }

    // Atualização parcial: só muda o que veio no corpo; "" apaga o campo de texto
    @Transactional
    public MentorResposta atualizar(Long id, AtualizacaoMentor dados) {
        Mentor m = buscarEntidade(id);
        if (dados.nome() != null) m.setNome(dados.nome().trim());
        if (dados.bio() != null) m.setBio(vazioViraNulo(dados.bio()));
        if (dados.areasAtuacao() != null) m.setAreasAtuacao(vazioViraNulo(dados.areasAtuacao()));
        if (dados.ecossistemas() != null) m.setEcossistemas(vazioViraNulo(dados.ecossistemas()));
        if (dados.temas() != null) m.setTemas(dados.temas());
        if (dados.anosExperiencia() != null) m.setAnosExperiencia(dados.anosExperiencia());
        if (dados.valorSessao() != null) m.setValorSessao(dados.valorSessao());
        if (dados.duracaoSessaoMinutos() != null) m.setDuracaoSessaoMinutos(dados.duracaoSessaoMinutos());
        if (dados.fotoUrl() != null) m.setFotoUrl(vazioViraNulo(dados.fotoUrl()));
        if (dados.videoUrl() != null) m.setVideoUrl(vazioViraNulo(dados.videoUrl()));
        if (dados.linkedinUrl() != null) m.setLinkedinUrl(vazioViraNulo(dados.linkedinUrl()));
        if (dados.whatsapp() != null) m.setWhatsapp(dados.whatsapp());
        if (dados.compartilharWhatsapp() != null) m.setCompartilharWhatsapp(dados.compartilharWhatsapp());
        mentorRepository.save(m);
        if (dados.duracaoSessaoMinutos() != null) {
            indicadores.recalcular(id); // outra duração gera outros blocos na agenda
        }
        return buscar(id);
    }

    public Mentor buscarEntidade(Long id) {
        return mentorRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado(a)"));
    }

    static String vazioViraNulo(String texto) {
        return texto.isBlank() ? null : texto.trim();
    }

    private Reputacao reputacao() {
        Map<Long, Double> medias = new HashMap<>();
        Map<Long, Long> avaliacoes = new HashMap<>();
        for (Object[] linha : avaliacaoRepository.resumoPorMentor()) {
            Long mentorId = (Long) linha[0];
            double media = ((Number) linha[1]).doubleValue();
            medias.put(mentorId, BigDecimal.valueOf(media).setScale(1, RoundingMode.HALF_UP).doubleValue());
            avaliacoes.put(mentorId, ((Number) linha[2]).longValue());
        }
        Map<Long, Long> realizadas = new HashMap<>();
        for (Object[] linha : agendamentoRepository.realizadasPorMentor(LocalDateTime.now(relogio))) {
            realizadas.put((Long) linha[0], ((Number) linha[1]).longValue());
        }
        return new Reputacao(medias, avaliacoes, realizadas);
    }

    private record Reputacao(Map<Long, Double> medias, Map<Long, Long> avaliacoes, Map<Long, Long> realizadas) {
        MentorResposta aplicar(Mentor m) {
            return MentorResposta.de(m, medias.get(m.getId()),
                    avaliacoes.getOrDefault(m.getId(), 0L), realizadas.getOrDefault(m.getId(), 0L));
        }
    }
}
