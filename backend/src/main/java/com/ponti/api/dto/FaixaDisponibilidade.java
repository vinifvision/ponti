package com.ponti.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ponti.api.model.Disponibilidade;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

// Uma faixa da disponibilidade semanal, como o front envia e recebe:
// { "diaSemana": "TUESDAY", "horaInicio": "09:00", "horaFim": "12:00" }
public record FaixaDisponibilidade(
        @NotNull DayOfWeek diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim) {

    @JsonIgnore
    @AssertTrue(message = "O horário de término precisa ser depois do horário de início")
    public boolean isIntervaloValido() {
        return horaInicio == null || horaFim == null || horaFim.isAfter(horaInicio);
    }

    public static FaixaDisponibilidade de(Disponibilidade d) {
        return new FaixaDisponibilidade(d.getDiaSemana(), d.getHoraInicio(), d.getHoraFim());
    }
}
