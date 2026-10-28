package com.ponti.api.pagamento;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PixBrCodeTest {

    @Test
    void crcConfereComOExemploDaEspecificacao() {
        // Valor de referência do CRC16-CCITT (0xFFFF) para "123456789"
        assertThat(PixBrCode.crc16("123456789")).isEqualTo("29B1");
    }

    @Test
    void codigoTemOsCamposDoBrCodeEUmCrcValido() {
        String codigo = PixBrCode.gerar("chave-teste", "Ponti Mentoria Simulado", "Recife", new BigDecimal("120"), "PNT7K2M9QX4");

        assertThat(codigo).startsWith("000201").contains("br.gov.bcb.pix").contains("5406120.00")
                .contains("5802BR").contains("PONTI MENTORIA SIMULADO").contains("RECIFE").contains("PNT7K2M9QX4");
        String semCrc = codigo.substring(0, codigo.length() - 4);
        assertThat(semCrc).endsWith("6304");
        assertThat(codigo.substring(codigo.length() - 4)).isEqualTo(PixBrCode.crc16(semCrc));
    }

    @Test
    void cartaoValidaPeloAlgoritmoDeLuhnEDescobreABandeira() {
        assertThat(GatewayPagamentoSimulado.luhnValido("4242424242424242")).isTrue();
        assertThat(GatewayPagamentoSimulado.luhnValido("4242424242424241")).isFalse();
        assertThat(GatewayPagamentoSimulado.bandeira("4242424242424242")).isEqualTo("Visa");
        assertThat(GatewayPagamentoSimulado.bandeira("5555555555554444")).isEqualTo("Mastercard");
        assertThat(GatewayPagamentoSimulado.bandeira("378282246310005")).isEqualTo("American Express");
    }
}
