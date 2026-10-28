package com.ponti.api.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;

// Monta um código Pix "copia e cola" no formato BR Code do Banco Central (EMV com CRC16).
// É o mesmo formato que vira QR Code nos apps de banco. Aqui ele é SIMULADO: a chave é fictícia
// e o nome do recebedor deixa claro que não há cobrança de verdade.
public final class PixBrCode {

    private PixBrCode() {
    }

    public static String gerar(String chave, String recebedor, String cidade, BigDecimal valor, String txid) {
        String contaPix = campo("00", "br.gov.bcb.pix") + campo("01", chave);
        String semCrc = campo("00", "01")
                + campo("26", contaPix)
                + campo("52", "0000")
                + campo("53", "986")
                + campo("54", valor.setScale(2, RoundingMode.HALF_UP).toPlainString())
                + campo("58", "BR")
                + campo("59", limpar(recebedor, 25))
                + campo("60", limpar(cidade, 15))
                + campo("62", campo("05", limpar(txid, 25).replace(" ", "")))
                + "6304";
        return semCrc + crc16(semCrc);
    }

    private static String campo(String id, String valor) {
        return id + String.format("%02d", valor.length()) + valor;
    }

    // BR Code só aceita caracteres simples: tira acentos, deixa em maiúsculas e corta no tamanho
    private static String limpar(String texto, int maximo) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String limpo = semAcento.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 \\-]", "");
        return limpo.length() > maximo ? limpo.substring(0, maximo) : limpo;
    }

    // CRC16-CCITT (polinômio 0x1021, valor inicial 0xFFFF), como pede a especificação do BR Code
    static String crc16(String dados) {
        int crc = 0xFFFF;
        for (byte b : dados.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
