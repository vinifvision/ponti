package com.ponti.api.service;

import com.ponti.api.model.Agendamento;
import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.AvaliacaoRepository;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.ConviteRepository;
import com.ponti.api.repository.DisponibilidadeRepository;
import com.ponti.api.repository.MentorIndicadoresRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Exclusão de conta: apaga o perfil e tudo que depende dele (login, agendamentos,
// sugestões, certificações, agenda e avaliações), numa transação só, para não sobrar dado solto.
@Service
public class ContaService {

    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;
    private final UsuarioRepository usuarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final ConviteRepository conviteRepository;
    private final CertificacaoRepository certificacaoRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final MentorIndicadoresRepository indicadoresRepository;
    private final IndicadoresMentorService indicadores;

    public ContaService(StartupRepository startupRepository,
                        MentorRepository mentorRepository,
                        UsuarioRepository usuarioRepository,
                        AgendamentoRepository agendamentoRepository,
                        ConviteRepository conviteRepository,
                        CertificacaoRepository certificacaoRepository,
                        DisponibilidadeRepository disponibilidadeRepository,
                        AvaliacaoRepository avaliacaoRepository,
                        MentorIndicadoresRepository indicadoresRepository,
                        IndicadoresMentorService indicadores) {
        this.startupRepository = startupRepository;
        this.mentorRepository = mentorRepository;
        this.usuarioRepository = usuarioRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.conviteRepository = conviteRepository;
        this.certificacaoRepository = certificacaoRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.indicadoresRepository = indicadoresRepository;
        this.indicadores = indicadores;
    }

    @Transactional
    public void excluirStartup(Long startupId) {
        if (!startupRepository.existsById(startupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrada");
        }
        // Mentores que tinham pedidos ou avaliações desta startup: os indicadores deles mudam
        Set<Long> mentoresAfetados = agendamentoRepository.findByStartupIdOrderByInicioAsc(startupId).stream()
                .map(Agendamento::getMentorId).collect(Collectors.toSet());
        avaliacaoRepository.deleteByStartupId(startupId);
        agendamentoRepository.deleteByStartupId(startupId);
        conviteRepository.deleteByStartupId(startupId);
        certificacaoRepository.deleteByStartupId(startupId);
        usuarioRepository.deleteByStartupId(startupId);
        startupRepository.deleteById(startupId);
        mentoresAfetados.forEach(indicadores::recalcular);
    }

    @Transactional
    public void excluirMentor(Long mentorId) {
        if (!mentorRepository.existsById(mentorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado(a)");
        }
        avaliacaoRepository.deleteByMentorId(mentorId);
        agendamentoRepository.deleteByMentorId(mentorId);
        conviteRepository.deleteByMentorId(mentorId);
        certificacaoRepository.deleteByMentorId(mentorId);
        disponibilidadeRepository.deleteByMentorId(mentorId);
        usuarioRepository.deleteByMentorId(mentorId);
        indicadoresRepository.deleteById(mentorId);
        mentorRepository.deleteById(mentorId);
    }
}
