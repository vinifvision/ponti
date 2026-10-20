package com.ponti.api.controller;

import com.ponti.api.dto.ConviteDetalhe;
import com.ponti.api.model.Convite;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.model.StatusConvite;
import com.ponti.api.repository.ConviteRepository;
import com.ponti.api.repository.MentorRepository;
import com.ponti.api.repository.StartupRepository;
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

    public ConviteController(ConviteRepository repository,
                             StartupRepository startupRepository,
                             MentorRepository mentorRepository) {
        this.repository = repository;
        this.startupRepository = startupRepository;
        this.mentorRepository = mentorRepository;
    }

    // READ (todos), com filtros opcionais:
    //   /convites?mentorId=1            -> inbox do mentor
    //   /convites?mentorId=1&status=PENDENTE
    //   /convites?startupId=2           -> convites enviados pela startup
    @GetMapping
    public List<Convite> listar(@RequestParam(required = false) Long mentorId,
                                @RequestParam(required = false) Long startupId,
                                @RequestParam(required = false) StatusConvite status) {
        if (mentorId != null) {
            return status != null
                    ? repository.findByMentorIdAndStatus(mentorId, status)
                    : repository.findByMentorId(mentorId);
        }
        if (startupId != null) {
            return status != null
                    ? repository.findByStartupIdAndStatus(startupId, status)
                    : repository.findByStartupId(startupId);
        }
        return status != null ? repository.findByStatus(status) : repository.findAll();
    }

    // READ (todos), já com os dados da startup e do mentor. Aceita os mesmos filtros de GET /convites.
    //   /convites/detalhados?mentorId=1&status=PENDENTE  -> inbox do mentor
    //   /convites/detalhados?startupId=2                 -> enviados pela startup
    @GetMapping("/detalhados")
    public List<ConviteDetalhe> listarDetalhados(@RequestParam(required = false) Long mentorId,
                                                 @RequestParam(required = false) Long startupId,
                                                 @RequestParam(required = false) StatusConvite status) {
        return listar(mentorId, startupId, status).stream().map(this::detalhar).toList();
    }

    // READ (um)
    @GetMapping("/{id}")
    public Convite buscar(@PathVariable Long id) {
        return procurar(id);
    }

    // CREATE: a startup envia um convite para um mentor (sempre nasce PENDENTE)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Convite criar(@Valid @RequestBody Convite body) {
        if (!startupRepository.existsById(body.getStartupId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrada");
        }
        if (!mentorRepository.existsById(body.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado");
        }
        if (repository.existsByStartupIdAndMentorId(body.getStartupId(), body.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Essa startup já enviou um convite para esse mentor");
        }
        body.setId(null);
        body.setStatus(StatusConvite.PENDENTE);
        return repository.save(body);
    }

    // O mentor aceita o convite
    @PostMapping("/{id}/aceitar")
    public Convite aceitar(@PathVariable Long id) {
        return responder(id, StatusConvite.ACEITO);
    }

    // O mentor recusa o convite
    @PostMapping("/{id}/recusar")
    public Convite recusar(@PathVariable Long id) {
        return responder(id, StatusConvite.RECUSADO);
    }

    // UPDATE
    @PutMapping("/{id}")
    public Convite atualizar(@PathVariable Long id, @Valid @RequestBody Convite body) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado(a)");
        }
        body.setId(id);
        return repository.save(body);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado(a)");
        }
        repository.deleteById(id);
    }

    private ConviteDetalhe detalhar(Convite convite) {
        Startup s = startupRepository.findById(convite.getStartupId()).orElse(null);
        Mentor m = mentorRepository.findById(convite.getMentorId()).orElse(null);
        boolean aceito = convite.getStatus() == StatusConvite.ACEITO;

        ConviteDetalhe.StartupResumo startup = s == null ? null : new ConviteDetalhe.StartupResumo(
                s.getId(), s.getNome(), s.getDescricao(), s.getSegmento(), s.getCidade(), s.getEstado(),
                s.getEstagioMaturidade(), s.getVideoPitchUrl(), aceito ? s.getSite() : null);
        ConviteDetalhe.MentorResumo mentor = m == null ? null : new ConviteDetalhe.MentorResumo(
                m.getId(), m.getNome(), m.getAreasAtuacao(), m.getEcossistemas(), m.getValorSessao());

        return new ConviteDetalhe(convite.getId(), convite.getStatus(), startup, mentor);
    }

    private Convite procurar(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado(a)"));
    }

    // Só convites PENDENTES podem ser respondidos, e só uma vez
    private Convite responder(Long id, StatusConvite novoStatus) {
        Convite convite = procurar(id);
        if (convite.getStatus() != StatusConvite.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse convite já foi respondido (" + convite.getStatus() + ")");
        }
        convite.setStatus(novoStatus);
        return repository.save(convite);
    }
}
