package com.ponti.api.controller;

import com.ponti.api.dto.AgendamentoDetalhe;
import com.ponti.api.dto.ConfirmacaoAgendamento;
import com.ponti.api.dto.FaixaDisponibilidade;
import com.ponti.api.dto.NovoAgendamento;
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
// TODO (PR de segurança): conferir pelo token quem está agindo em cada rota.
@RestController
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
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
        return agendaService.definirDisponibilidade(id, faixas);
    }

    // Horários livres para agendar, ex.: /mentores/1/horarios?de=2026-11-01&ate=2026-11-30
    @GetMapping("/mentores/{id}/horarios")
    public List<LocalDateTime> horariosLivres(@PathVariable Long id,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return agendaService.horariosLivres(id, de, ate);
    }

    // Mentorias de uma startup ou de um mentor: /agendamentos?startupId=1 ou /agendamentos?mentorId=2
    @GetMapping("/agendamentos")
    public List<AgendamentoDetalhe> listar(@RequestParam(required = false) Long startupId,
                                           @RequestParam(required = false) Long mentorId) {
        return agendaService.listar(startupId, mentorId);
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoDetalhe agendar(@Valid @RequestBody NovoAgendamento body) {
        return agendaService.detalhar(agendaService.agendar(body));
    }

    @PostMapping("/agendamentos/{id}/confirmar")
    public AgendamentoDetalhe confirmar(@PathVariable Long id, @Valid @RequestBody ConfirmacaoAgendamento body) {
        return agendaService.detalhar(agendaService.confirmar(id, body.linkReuniao()));
    }

    @PostMapping("/agendamentos/{id}/recusar")
    public AgendamentoDetalhe recusar(@PathVariable Long id) {
        return agendaService.detalhar(agendaService.recusar(id));
    }

    @PostMapping("/agendamentos/{id}/cancelar")
    public AgendamentoDetalhe cancelar(@PathVariable Long id) {
        return agendaService.detalhar(agendaService.cancelar(id));
    }
}
