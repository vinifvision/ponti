package com.ponti.api.service;

import com.ponti.api.dto.AvaliacaoPublica;
import com.ponti.api.dto.NovaAvaliacao;
import com.ponti.api.model.Agendamento;
import com.ponti.api.model.Avaliacao;
import com.ponti.api.model.Startup;
import com.ponti.api.repository.AvaliacaoRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Avaliações: depois de uma mentoria realizada, a startup dá nota de 1 a 5 e um comentário.
@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;
    private final AgendaService agendaService;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository, StartupRepository startupRepository,
                            MentorRepository mentorRepository, AgendaService agendaService) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.startupRepository = startupRepository;
        this.mentorRepository = mentorRepository;
        this.agendaService = agendaService;
    }

    @Transactional
    public Avaliacao avaliar(Agendamento agendamento, NovaAvaliacao dados) {
        if (!agendaService.realizada(agendamento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Só dá para avaliar depois que a mentoria acontecer");
        }
        if (avaliacaoRepository.findByAgendamentoId(agendamento.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa mentoria já foi avaliada");
        }
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setAgendamentoId(agendamento.getId());
        avaliacao.setMentorId(agendamento.getMentorId());
        avaliacao.setStartupId(agendamento.getStartupId());
        avaliacao.setNota(dados.nota());
        avaliacao.setComentario(dados.comentario() == null || dados.comentario().isBlank() ? null : dados.comentario().trim());
        return avaliacaoRepository.save(avaliacao);
    }

    public List<AvaliacaoPublica> doMentor(Long mentorId) {
        if (!mentorRepository.existsById(mentorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado(a)");
        }
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByMentorIdOrderByCriadoEmDesc(mentorId);
        Map<Long, Startup> startups = startupRepository
                .findAllById(avaliacoes.stream().map(Avaliacao::getStartupId).distinct().toList()).stream()
                .collect(Collectors.toMap(Startup::getId, Function.identity()));
        return avaliacoes.stream().map(a -> {
            Startup s = startups.get(a.getStartupId());
            return new AvaliacaoPublica(a.getId(), a.getNota(), a.getComentario(),
                    s == null ? "Startup" : s.getNome(), s == null ? null : s.getFotoUrl(), a.getCriadoEm());
        }).toList();
    }
}
