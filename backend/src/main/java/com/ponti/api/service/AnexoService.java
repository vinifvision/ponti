package com.ponti.api.service;

import com.ponti.api.arquivos.ArmazenamentoArquivos;
import com.ponti.api.dto.AnexoResposta;
import com.ponti.api.model.Anexo;
import com.ponti.api.repository.AnexoRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

// Anexos das atividades (mentor) e das entregas (startup).
// Regras (as mesmas do front, conferidas de novo aqui, porque o front pode ser burlado):
//  - até 5 arquivos por envio, até 10 MB cada, sem arquivo vazio;
//  - só as extensões aceitas, e o conteúdo precisa combinar com a extensão (um .exe renomeado
//    para .pdf é recusado);
//  - o tipo do conteúdo é definido pelo servidor a partir da extensão, nunca pelo que o navegador mandou.
@Service
public class AnexoService {

    private static final Logger log = LoggerFactory.getLogger(AnexoService.class);

    public static final int MAX_ARQUIVOS = 5;
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    static final Map<String, String> TIPOS = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("zip", "application/zip"));

    private final AnexoRepository repository;
    private final ArmazenamentoArquivos armazenamento;

    public AnexoService(AnexoRepository repository, ArmazenamentoArquivos armazenamento) {
        this.repository = repository;
        this.armazenamento = armazenamento;
    }

    // ----- validação -----

    public static List<MultipartFile> naoVazios(List<MultipartFile> arquivos) {
        return arquivos == null ? List.of() : arquivos.stream().filter(a -> a != null && a.getOriginalFilename() != null).toList();
    }

    public void validar(List<MultipartFile> arquivos) {
        if (arquivos.size() > MAX_ARQUIVOS) {
            throw erro("Dá para anexar até " + MAX_ARQUIVOS + " arquivos.");
        }
        for (MultipartFile arquivo : arquivos) {
            String nome = nomeLimpo(arquivo.getOriginalFilename());
            String extensao = extensao(nome);
            if (!TIPOS.containsKey(extensao)) {
                throw erro("\"" + nome + "\" não é um tipo aceito. Use " + String.join(", ", TIPOS.keySet().stream().sorted().toList()) + ".");
            }
            if (arquivo.isEmpty()) throw erro("\"" + nome + "\" está vazio.");
            if (arquivo.getSize() > MAX_BYTES) throw erro("\"" + nome + "\" passa de 10 MB.");
            if (!conteudoCombina(extensao, ler(arquivo))) {
                throw erro("O conteúdo de \"" + nome + "\" não combina com a extensão do arquivo.");
            }
        }
    }

    // Confere a "assinatura" do começo do arquivo (os primeiros bytes de cada formato são sempre iguais)
    static boolean conteudoCombina(String extensao, byte[] b) {
        return switch (extensao) {
            case "pdf" -> comeca(b, 0x25, 0x50, 0x44, 0x46);                       // %PDF
            case "png" -> comeca(b, 0x89, 0x50, 0x4E, 0x47);                       // .PNG
            case "jpg", "jpeg" -> comeca(b, 0xFF, 0xD8, 0xFF);
            case "docx", "xlsx", "pptx", "zip" -> comeca(b, 0x50, 0x4B, 0x03, 0x04); // PK (formato zip)
            case "doc", "xls", "ppt" -> comeca(b, 0xD0, 0xCF, 0x11, 0xE0);          // formato antigo do Office
            case "txt", "csv" -> semBytesNulos(b);                                // texto não tem byte zero
            default -> false;
        };
    }

    // ----- gravar -----

    public void salvarDaAtividade(Long acompanhamentoId, Long atividadeId, List<MultipartFile> arquivos) {
        salvar(acompanhamentoId, atividadeId, null, arquivos);
    }

    public void salvarDaEntrega(Long acompanhamentoId, Long entregaId, List<MultipartFile> arquivos) {
        salvar(acompanhamentoId, null, entregaId, arquivos);
    }

    private void salvar(Long acompanhamentoId, Long atividadeId, Long entregaId, List<MultipartFile> arquivos) {
        List<String> enviados = new ArrayList<>();
        // Se algo der errado depois (e o banco desfizer tudo), apaga do armazenamento o que já subiu
        seODesfazer(() -> armazenamento.apagar(enviados));
        for (MultipartFile arquivo : arquivos) {
            String nome = nomeLimpo(arquivo.getOriginalFilename());
            String extensao = extensao(nome);
            String caminho = "acompanhamentos/" + acompanhamentoId + "/" + UUID.randomUUID() + "." + extensao;
            try {
                armazenamento.salvar(caminho, arquivo.getBytes(), TIPOS.get(extensao));
            } catch (IOException | RuntimeException e) {
                log.error("Falha ao guardar o arquivo {}", caminho, e);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Não foi possível guardar o arquivo \"" + nome + "\". Tente novamente.");
            }
            enviados.add(caminho);
            Anexo anexo = new Anexo();
            anexo.setAcompanhamentoId(acompanhamentoId);
            anexo.setAtividadeId(atividadeId);
            anexo.setEntregaId(entregaId);
            anexo.setNome(nome);
            anexo.setTamanho(arquivo.getSize());
            anexo.setTipoConteudo(TIPOS.get(extensao));
            anexo.setCaminho(caminho);
            repository.save(anexo);
        }
    }

    // ----- mostrar -----

    public List<AnexoResposta> daAtividade(Long atividadeId) {
        return respostas(repository.findByAtividadeIdOrderByIdAsc(atividadeId));
    }

    public List<AnexoResposta> daEntrega(Long entregaId) {
        return respostas(repository.findByEntregaIdOrderByIdAsc(entregaId));
    }

    private List<AnexoResposta> respostas(List<Anexo> anexos) {
        return anexos.stream().map(a -> new AnexoResposta(a.getNome(), a.getTamanho(), link(a))).toList();
    }

    // Se o armazenamento estiver fora do ar, o anexo aparece sem link em vez de quebrar a tela toda
    private String link(Anexo a) {
        try {
            return armazenamento.linkTemporario(a.getCaminho(), a.getNome());
        } catch (RuntimeException e) {
            log.warn("Não foi possível gerar o link de {}", a.getCaminho(), e);
            return null;
        }
    }

    // ----- apagar -----

    public void apagarDaEntrega(Long entregaId) {
        apagar(repository.findByEntregaIdOrderByIdAsc(entregaId));
    }

    public void apagarDoAcompanhamento(Long acompanhamentoId) {
        apagar(repository.findByAcompanhamentoId(acompanhamentoId));
    }

    private void apagar(List<Anexo> anexos) {
        if (anexos.isEmpty()) return;
        List<String> caminhos = anexos.stream().map(Anexo::getCaminho).toList();
        repository.deleteAll(anexos);
        // Só apaga os arquivos depois que o banco confirmou (se a exclusão for desfeita, os arquivos continuam lá)
        depoisDeConfirmar(() -> {
            try {
                armazenamento.apagar(caminhos);
            } catch (RuntimeException e) {
                log.warn("Não foi possível apagar arquivos do armazenamento: {}", caminhos, e);
            }
        });
    }

    // ----- apoio -----

    private static void depoisDeConfirmar(Runnable acao) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            acao.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                acao.run();
            }
        });
    }

    private static void seODesfazer(Runnable acao) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        acao.run();
                    } catch (RuntimeException e) {
                        log.warn("Não foi possível limpar arquivos depois de um erro", e);
                    }
                }
            }
        });
    }

    // Tira pastas do nome ("C:\\fotos\\a.png" -> "a.png") e limita o tamanho
    static String nomeLimpo(String original) {
        String nome = original == null ? "arquivo" : original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (nome.isEmpty()) nome = "arquivo";
        return nome.length() > 200 ? nome.substring(nome.length() - 200) : nome;
    }

    static String extensao(String nome) {
        int i = nome.lastIndexOf('.');
        return i < 0 ? "" : nome.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] ler(MultipartFile arquivo) {
        try {
            byte[] tudo = arquivo.getBytes();
            return Arrays.copyOf(tudo, Math.min(tudo.length, 1024));
        } catch (IOException e) {
            throw erro("Não foi possível ler o arquivo.");
        }
    }

    private static boolean comeca(byte[] b, int... assinatura) {
        if (b.length < assinatura.length) return false;
        for (int i = 0; i < assinatura.length; i++) {
            if ((b[i] & 0xFF) != assinatura[i]) return false;
        }
        return true;
    }

    private static boolean semBytesNulos(byte[] b) {
        for (byte x : b) {
            if (x == 0) return false;
        }
        return true;
    }

    private static ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
