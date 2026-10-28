package com.ponti.api.controller;

import com.ponti.api.dto.ConviteDetalhe;
import com.ponti.api.model.Convite;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.model.StatusConvite;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.repository.ConviteRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
import com.ponti.api.seguranca.Acesso;
import com.ponti.api.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/convites")
public class ConviteController {

    private final ConviteRepository repository;
    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;
    private final Acesso acesso;

    public ConviteController(ConviteRepository repository,
                             StartupRepository startupRepository,
                             MentorRepository mentorRepository,
                             Acesso acesso) {
        this.repository = repository;
        this.startupRepository = startupRepository;
        this.mentorRepository = mentorRepository;
        this.acesso = acesso;
    }

    // Convites de quem está logado (a startup vê os dela; o mentor, os dele), com filtro opcional de status.
    // Os parâmetros startupId/mentorId continuam aceitos, mas só para o próprio perfil.
    //   /convites                 -> convites do usuário logado
    //   /convites?status=PENDENTE -> só os pendentes
    @GetMapping
    public List<Convite> listar(@RequestParam(required = false) Long mentorId,
                                @RequestParam(required = false) Long startupId,
                                @RequestParam(required = false) StatusConvite status) {
        UsuarioLogado logado = acesso.atual();
        if (mentorId != null || (startupId == null && logado.mentorId() != null)) {
            Long id = mentorId != null ? mentorId : logado.mentorId();
            acesso.exigirMentor(id);
            return status != null ? repository.findByMentorIdAndStatus(id, status) : repository.findByMentorId(id);
        }
        Long id = startupId != null ? startupId : logado.startupId();
        acesso.exigirStartup(id);
        return status != null ? repository.findByStartupIdAndStatus(id, status) : repository.findByStartupId(id);
    }

    // READ (todos), já com os dados da startup e do mentor. Aceita os mesmos filtros de GET /convites.
    //   /convites/detalhados?mentorId=1   -> convites do mentor (recebidos e enviados)
    //   /convites/detalhados?startupId=2  -> convites da startup (recebidos e enviados)
    // O campo enviadoPor diz de quem foi a iniciativa.
    @GetMapping("/detalhados")
    public List<ConviteDetalhe> listarDetalhados(@RequestParam(required = false) Long mentorId,
                                                 @RequestParam(required = false) Long startupId,
                                                 @RequestParam(required = false) StatusConvite status) {
        return listar(mentorId, startupId, status).stream().map(this::detalhar).toList();
    }

    // READ (um): só a startup e o mentor do convite
    @GetMapping("/{id}")
    public Convite buscar(@PathVariable Long id) {
        Convite convite = procurar(id);
        acesso.exigirParticipante(convite.getStartupId(), convite.getMentorId());
        return convite;
    }

    // CREATE: a startup convida um mentor, ou o mentor convida uma startup (enviadoPor).
    // Sempre nasce PENDENTE. Só existe um convite por par startup + mentor, seja qual for o sentido.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Convite criar(@Valid @RequestBody Convite body) {
        if (body.getEnviadoPor() == null) {
            body.setEnviadoPor(TipoUsuario.STARTUP);
        }
        // Quem envia precisa ser o lado que aparece como remetente
        if (body.getEnviadoPor() == TipoUsuario.MENTOR) {
            acesso.exigirMentor(body.getMentorId());
        } else {
            acesso.exigirStartup(body.getStartupId());
        }
        if (!startupRepository.existsById(body.getStartupId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrada");
        }
        if (!mentorRepository.existsById(body.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado");
        }
        if (repository.existsByStartupIdAndMentorId(body.getStartupId(), body.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe um convite entre essa startup e esse mentor");
        }
        body.setId(null);
        body.setStatus(StatusConvite.PENDENTE);
        return repository.save(body);
    }

    // Quem recebeu o convite aceita (o lado que não enviou)
    @PostMapping("/{id}/aceitar")
    public Convite aceitar(@PathVariable Long id) {
        return responder(id, StatusConvite.ACEITO);
    }

    // Quem recebeu o convite recusa
    @PostMapping("/{id}/recusar")
    public Convite recusar(@PathVariable Long id) {
        return responder(id, StatusConvite.RECUSADO);
    }

    private ConviteDetalhe detalhar(Convite convite) {
        Startup s = startupRepository.findById(convite.getStartupId()).orElse(null);
        Mentor m = mentorRepository.findById(convite.getMentorId()).orElse(null);
        boolean aceito = convite.getStatus() == StatusConvite.ACEITO;

        ConviteDetalhe.StartupResumo startup = s == null ? null : new ConviteDetalhe.StartupResumo(
                s.getId(), s.getNome(), s.getDescricao(), s.getSegmento(), s.getCidade(), s.getEstado(),
                s.getEstagioMaturidade(), s.getVideoPitchUrl(), s.getFotoUrl(), aceito ? s.getSite() : null);
        ConviteDetalhe.MentorResumo mentor = m == null ? null : new ConviteDetalhe.MentorResumo(
                m.getId(), m.getNome(), m.getAreasAtuacao(), m.getEcossistemas(), m.getValorSessao(),
                m.getDuracaoSessaoMinutos(), m.getFotoUrl(), m.getLinkedinUrl());

        return new ConviteDetalhe(convite.getId(), convite.getStatus(), convite.getEnviadoPor(), startup, mentor);
    }

    private Convite procurar(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado(a)"));
    }

    // Só convites PENDENTES podem ser respondidos, e só uma vez
    private Convite responder(Long id, StatusConvite novoStatus) {
        Convite convite = procurar(id);
        if (convite.getEnviadoPor() == TipoUsuario.MENTOR) {
            acesso.exigirStartup(convite.getStartupId());
        } else {
            acesso.exigirMentor(convite.getMentorId());
        }
        if (convite.getStatus() != StatusConvite.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse convite já foi respondido (" + convite.getStatus() + ")");
        }
        convite.setStatus(novoStatus);
        return repository.save(convite);
    }
}
