package com.ponti.api.controller;

import com.ponti.api.dto.AgendamentoDetalhe;
import com.ponti.api.dto.ConfirmacaoAgendamento;
import com.ponti.api.dto.FaixaDisponibilidade;
import com.ponti.api.dto.NovoAgendamento;
import com.ponti.api.model.Agendamento;
import com.ponti.api.seguranca.Acesso;
import com.ponti.api.seguranca.UsuarioLogado;
import com.ponti.api.service.AgendaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Agenda de mentorias (fluxo parecido com o do Preply):
//   1. o mentor define os horários em que atende na semana;
//   2. a startup vê os horários livres e agenda um (pagamento simulado);
//   3. o mentor confirma com o link do Google Meet, ou recusa.
// Todas as rotas exigem login; quem pode fazer cada ação é conferido pela classe Acesso.
@RestController
public class AgendaController {

    private final AgendaService agendaService;
    private final Acesso acesso;

    public AgendaController(AgendaService agendaService, Acesso acesso) {
        this.agendaService = agendaService;
        this.acesso = acesso;
    }

    @GetMapping("/mentores/{id}/disponibilidade")
    public List<FaixaDisponibilidade> disponibilidade(@PathVariable Long id) {
        return agendaService.disponibilidade(id);
    }

    // Substitui a disponibilidade semanal inteira do mentor
    @PutMapping("/mentores/{id}/disponibilidade")
    public List<FaixaDisponibilidade> definirDisponibilidade(
            @PathVariable Long id,
            @RequestBody @Size(max = 50, message = "No máximo 50 faixas de horário") List<@Valid FaixaDisponibilidade> faixas) {
        acesso.exigirMentor(id);
        return agendaService.definirDisponibilidade(id, faixas);
    }

    // Horários livres para agendar, ex.: /mentores/1/horarios?de=2026-11-01&ate=2026-11-30
    @GetMapping("/mentores/{id}/horarios")
    public List<LocalDateTime> horariosLivres(@PathVariable Long id,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return agendaService.horariosLivres(id, de, ate);
    }

    // Mentorias de quem está logado. Os parâmetros startupId/mentorId são aceitos só para o próprio perfil.
    @GetMapping("/agendamentos")
    public List<AgendamentoDetalhe> listar(@RequestParam(required = false) Long startupId,
                                           @RequestParam(required = false) Long mentorId) {
        UsuarioLogado logado = acesso.atual();
        if (mentorId != null || (startupId == null && logado.mentorId() != null)) {
            Long id = mentorId != null ? mentorId : logado.mentorId();
            acesso.exigirMentor(id);
            return agendaService.listar(null, id);
        }
        Long id = startupId != null ? startupId : logado.startupId();
        acesso.exigirStartup(id);
        return agendaService.listar(id, null);
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoDetalhe agendar(@Valid @RequestBody NovoAgendamento body) {
        acesso.exigirStartup(body.startupId()); // a startup só agenda em nome dela
        return agendaService.detalhar(agendaService.agendar(body));
    }

    @PostMapping("/agendamentos/{id}/confirmar")
    public AgendamentoDetalhe confirmar(@PathVariable Long id, @Valid @RequestBody ConfirmacaoAgendamento body) {
        acesso.exigirMentor(agendaService.buscar(id).getMentorId());
        return agendaService.detalhar(agendaService.confirmar(id, body.linkReuniao()));
    }

    @PostMapping("/agendamentos/{id}/recusar")
    public AgendamentoDetalhe recusar(@PathVariable Long id) {
        acesso.exigirMentor(agendaService.buscar(id).getMentorId());
        return agendaService.detalhar(agendaService.recusar(id));
    }

    @PostMapping("/agendamentos/{id}/cancelar")
    public AgendamentoDetalhe cancelar(@PathVariable Long id) {
        Agendamento agendamento = agendaService.buscar(id);
        acesso.exigirParticipante(agendamento.getStartupId(), agendamento.getMentorId());
        return agendaService.detalhar(agendaService.cancelar(id));
    }
}
