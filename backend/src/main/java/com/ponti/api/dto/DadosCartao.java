package com.ponti.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Dados do cartão enviados no agendamento pago com CARTAO. Só passam pelo gateway simulado:
// o backend guarda apenas a bandeira e os 4 últimos dígitos.
public record DadosCartao(
        @NotBlank @Pattern(regexp = "^[\\d ]{13,23}$", message = "deve ter entre 13 e 19 dígitos") String numero,
        @NotBlank @Size(max = 60) String nomeImpresso,
        @NotBlank @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{2}$", message = "use o formato MM/AA") String validade,
        @NotBlank @Pattern(regexp = "^\\d{3,4}$", message = "deve ter 3 ou 4 dígitos") String cvv) {

    @Override
    public String toString() {
        // Nunca escrever número completo ou CVV em log
        return "DadosCartao[final=" + (numero == null ? "?" : numero.replaceAll("\\D", "").replaceAll(".*(\\d{4})$", "$1")) + "]";
    }
}
