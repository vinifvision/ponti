package com.ponti.api.service;

import com.ponti.api.repository.AgendamentoRepository;
import com.ponti.api.repository.AvaliacaoRepository;
import com.ponti.api.repository.CertificacaoRepository;
import com.ponti.api.repository.ConviteRepository;
import com.ponti.api.repository.DisponibilidadeRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.repository.UsuarioRepository;
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

    public ContaService(StartupRepository startupRepository,
                        MentorRepository mentorRepository,
                        UsuarioRepository usuarioRepository,
                        AgendamentoRepository agendamentoRepository,
                        ConviteRepository conviteRepository,
                        CertificacaoRepository certificacaoRepository,
                        DisponibilidadeRepository disponibilidadeRepository,
                        AvaliacaoRepository avaliacaoRepository) {
        this.startupRepository = startupRepository;
        this.mentorRepository = mentorRepository;
        this.usuarioRepository = usuarioRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.conviteRepository = conviteRepository;
        this.certificacaoRepository = certificacaoRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    @Transactional
    public void excluirStartup(Long startupId) {
        if (!startupRepository.existsById(startupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrada");
        }
        avaliacaoRepository.deleteByStartupId(startupId);
        agendamentoRepository.deleteByStartupId(startupId);
        conviteRepository.deleteByStartupId(startupId);
        certificacaoRepository.deleteByStartupId(startupId);
        usuarioRepository.deleteByStartupId(startupId);
        startupRepository.deleteById(startupId);
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
        mentorRepository.deleteById(mentorId);
    }
}
