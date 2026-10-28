package com.ponti.api.pagamento;

import com.ponti.api.dto.DadosCartao;
import com.ponti.api.model.Agendamento;
import com.ponti.api.model.StatusAgendamento;
import com.ponti.api.model.StatusPagamento;
import com.ponti.api.repository.AgendamentoRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Gateway de pagamento SIMULADO. Imita o comportamento de um gateway real (como Mercado Pago ou Stripe)
// sem cobrar nada, para a demonstração ficar parecida com uma plataforma de verdade:
//   - Pix: gera o código "copia e cola" (BR Code) e, alguns segundos depois, "o banco confirma" sozinho;
//     se não for pago no prazo, o pedido expira e libera o horário.
//   - Cartão: valida número (algoritmo de Luhn), validade e CVV; o cartão de teste terminado em 0002
//     é recusado, para mostrar o caminho de erro.
// Para trocar por um gateway real, basta reimplementar esta classe mantendo os mesmos métodos.
@Component
public class GatewayPagamentoSimulado {

    private static final Logger log = LoggerFactory.getLogger(GatewayPagamentoSimulado.class);
    private static final String LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    // Chave Pix fictícia (aleatória, não pertence a ninguém)
    private static final String CHAVE_PIX_SIMULADA = UUID.randomUUID().toString();
    public static final String FINAL_CARTAO_RECUSADO = "0002";

    private final AgendamentoRepository agendamentoRepository;
    private final Clock relogio;
    private final SecureRandom aleatorio = new SecureRandom();
    private final Duration aprovacaoPix;
    private final Duration validadePix;

    public GatewayPagamentoSimulado(AgendamentoRepository agendamentoRepository, Clock relogio,
                                    @Value("${ponti.pagamento.pix-aprovacao-segundos:6}") long aprovacaoSegundos,
                                    @Value("${ponti.pagamento.pix-validade-minutos:15}") long validadeMinutos) {
        this.agendamentoRepository = agendamentoRepository;
        this.relogio = relogio;
        this.aprovacaoPix = Duration.ofSeconds(aprovacaoSegundos);
        this.validadePix = Duration.ofMinutes(validadeMinutos);
    }

    // ---------------------------------------------------------------- Pix

    // Gera a cobrança Pix: o agendamento fica AGUARDANDO até "o banco confirmar"
    public void iniciarPix(Agendamento agendamento) {
        String codigo = novoCodigoTransacao();
        LocalDateTime agora = LocalDateTime.now(relogio);
        agendamento.setCodigoTransacao(codigo);
        agendamento.setStatusPagamento(StatusPagamento.AGUARDANDO);
        agendamento.setPagamentoExpiraEm(agora.plus(validadePix));
        agendamento.setPixCopiaECola(PixBrCode.gerar(CHAVE_PIX_SIMULADA, "Ponti Mentoria Simulado", "Recife",
                agendamento.getValor(), codigo.replace("-", "")));
    }

    // Faz o papel do "webhook" do banco: a cada 2 segundos aprova os Pix gerados há mais de alguns segundos
    // e expira os que passaram do prazo (liberando o horário do mentor).
    @Scheduled(fixedDelayString = "${ponti.pagamento.verificar-ms:2000}")
    @Transactional
    public void processarPixPendentes() {
        LocalDateTime agora = LocalDateTime.now(relogio);
        for (Agendamento a : agendamentoRepository.findByStatusPagamento(StatusPagamento.AGUARDANDO)) {
            if (a.getPagamentoExpiraEm() != null && agora.isAfter(a.getPagamentoExpiraEm())) {
                a.setStatusPagamento(StatusPagamento.EXPIRADO);
                a.setStatus(StatusAgendamento.CANCELADO);
                a.setPixCopiaECola(null);
                log.info("Pix {} expirou; horário liberado", a.getCodigoTransacao());
            } else if (a.getPagamentoExpiraEm() != null
                    && !agora.isBefore(a.getPagamentoExpiraEm().minus(validadePix).plus(aprovacaoPix))) {
                confirmarPix(a);
            }
        }
    }

    public void confirmarPix(Agendamento agendamento) {
        agendamento.setStatusPagamento(StatusPagamento.PAGO);
        agendamento.setPagoEm(LocalDateTime.now(relogio));
        agendamento.setPixCopiaECola(null);
        agendamentoRepository.save(agendamento);
        log.info("Pix {} confirmado (simulado)", agendamento.getCodigoTransacao());
    }

    // ---------------------------------------------------------------- cartão

    // "Cobra" no cartão: valida os dados e aprova na hora, ou recusa
    public void cobrarCartao(Agendamento agendamento, DadosCartao cartao) {
        if (cartao == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dados do cartão");
        }
        String numero = cartao.numero().replaceAll("\\D", "");
        if (numero.length() < 13 || numero.length() > 19 || !luhnValido(numero)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Número do cartão inválido. Confira os dígitos.");
        }
        YearMonth validade = YearMonth.parse(cartao.validade(), DateTimeFormatter.ofPattern("MM/yy"));
        if (validade.isBefore(YearMonth.now(relogio))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cartão vencido. Use outro cartão.");
        }
        if (numero.endsWith(FINAL_CARTAO_RECUSADO)) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Pagamento recusado pelo banco emissor. Tente outro cartão ou pague com Pix.");
        }
        agendamento.setCodigoTransacao(novoCodigoTransacao());
        agendamento.setStatusPagamento(StatusPagamento.PAGO);
        agendamento.setPagoEm(LocalDateTime.now(relogio));
        agendamento.setCartaoFinal(numero.substring(numero.length() - 4));
        agendamento.setCartaoBandeira(bandeira(numero));
    }

    // ---------------------------------------------------------------- apoio

    static boolean luhnValido(String numero) {
        int soma = 0;
        boolean dobrar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int d = numero.charAt(i) - '0';
            if (dobrar) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            soma += d;
            dobrar = !dobrar;
        }
        return soma % 10 == 0;
    }

    public static String bandeira(String numero) {
        if (numero.matches("^(4011|4312|4389|4514|4576|5041|5066|5067|509|6277|6362|6363|650|6516|6550).*")) {
            return "Elo";
        }
        if (numero.startsWith("4")) {
            return "Visa";
        }
        if (numero.matches("^(5[1-5]|2[2-7]).*")) {
            return "Mastercard";
        }
        if (numero.matches("^3[47].*")) {
            return "American Express";
        }
        if (numero.matches("^(606282|3841).*")) {
            return "Hipercard";
        }
        return "Cartão";
    }

    private String novoCodigoTransacao() {
        StringBuilder sb = new StringBuilder("PNT-");
        for (int i = 0; i < 8; i++) {
            sb.append(LETRAS.charAt(aleatorio.nextInt(LETRAS.length())));
        }
        return sb.toString();
    }
}
