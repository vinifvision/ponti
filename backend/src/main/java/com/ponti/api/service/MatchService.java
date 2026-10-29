package com.ponti.api.service;

import com.ponti.api.dto.MentorRecomendado;
import com.ponti.api.dto.MentorResposta;
import com.ponti.api.dto.StartupRecomendada;
import com.ponti.api.model.Mentor;
import com.ponti.api.model.Startup;
import com.ponti.api.model.Tema;
import com.ponti.api.repository.StartupRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// MATCH entre startups e mentores. Calcula uma compatibilidade de 0 a 100 e explica o motivo.
//
// A regra é simples de propósito, para dar para explicar e conferir na mão:
//   - 75 pontos: quantos dos temas que a startup precisa o mentor cobre.
//                (startup precisa de 3 temas e o mentor cobre 2 -> 2/3 dos 75 pontos)
//   - 25 pontos: o mentor conhece o setor da startup (ex.: PetTech). Se ele atende "todas as áreas",
//                ganha metade desses pontos.
// O resultado vira uma porcentagem e vem com os motivos escritos, para a tela mostrar "por que combina".
@Service
public class MatchService {

    static final int PESO_TEMAS = 75;
    static final int PESO_SETOR = 25;

    private final MentorService mentorService;
    private final StartupRepository startupRepository;

    public MatchService(MentorService mentorService, StartupRepository startupRepository) {
        this.mentorService = mentorService;
        this.startupRepository = startupRepository;
    }

    // Resultado do cálculo: a nota (0 a 100) e os motivos em português
    public record Resultado(int pontos, List<String> motivos) {
    }

    // Mentores para a startup logada, do mais compatível para o menos
    public List<MentorRecomendado> mentoresParaStartup(Long startupId) {
        Startup startup = startupRepository.findById(startupId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrado(a)"));
        return mentorService.listar().stream()
                .map(m -> {
                    Resultado r = calcular(startup.getNecessidades(), startup.getSegmento(),
                            m.temas(), m.ecossistemas(), true);
                    return new MentorRecomendado(m, r.pontos(), r.motivos());
                })
                .sorted(Comparator.comparingInt(MentorRecomendado::compatibilidade).reversed()
                        .thenComparing((MentorRecomendado r) -> r.mentor().notaMedia() == null ? 0.0 : r.mentor().notaMedia(),
                                Comparator.reverseOrder())
                        .thenComparing(r -> r.mentor().nome()))
                .toList();
    }

    // Startups para o mentor logado, da mais compatível para a menos
    public List<StartupRecomendada> startupsParaMentor(Long mentorId) {
        MentorResposta mentor = mentorService.buscar(mentorId);
        return startupRepository.findAll().stream()
                .map(s -> {
                    Resultado r = calcular(s.getNecessidades(), s.getSegmento(),
                            mentor.temas(), mentor.ecossistemas(), false);
                    return new StartupRecomendada(s, r.pontos(), r.motivos());
                })
                .sorted(Comparator.comparingInt(StartupRecomendada::compatibilidade).reversed()
                        .thenComparing(r -> r.startup().getNome()))
                .toList();
    }

    // O cálculo em si. Não depende de banco, então dá para testar direto.
    // paraStartup = true: o texto fala com a startup ("Ajuda com..."); false: fala com o mentor ("Precisa de ajuda com...").
    static Resultado calcular(Set<Tema> necessidades, String segmento, Set<Tema> temasDoMentor,
                              String setoresDoMentor, boolean paraStartup) {
        Set<Tema> precisa = necessidades == null ? Set.of() : necessidades;
        Set<Tema> ajuda = temasDoMentor == null ? Set.of() : temasDoMentor;
        List<String> motivos = new ArrayList<>();

        // Temas em comum (mantém a ordem em que a startup escolheu)
        Set<Tema> emComum = new LinkedHashSet<>(precisa);
        emComum.retainAll(ajuda);

        double cobertura = precisa.isEmpty() ? 0 : (double) emComum.size() / precisa.size();

        if (precisa.isEmpty()) {
            motivos.add(paraStartup
                    ? "Escolha os temas em que você precisa de ajuda para ver a compatibilidade"
                    : "Esta startup ainda não escolheu os temas em que precisa de ajuda");
        } else if (emComum.isEmpty()) {
            motivos.add(paraStartup
                    ? "Não atua nos temas que você escolheu"
                    : "Você não atua nos temas que esta startup precisa");
        } else {
            String lista = juntar(emComum.stream().map(Tema::getRotulo).toList());
            String contagem = emComum.size() + " de " + precisa.size();
            motivos.add(paraStartup
                    ? "Ajuda com " + lista + " (" + contagem + " temas que você precisa)"
                    : "Precisa de ajuda com " + lista + " (" + contagem + " temas dela, que você domina)");
        }

        double setor = afinidadeDeSetor(segmento, setoresDoMentor);
        if (setor == 1.0) {
            motivos.add(paraStartup ? "Conhece o setor " + segmento.trim() : "Você atua no setor " + segmento.trim());
        } else if (setor > 0) {
            motivos.add(paraStartup ? "Atende todos os setores" : "Você atende todos os setores");
        }

        int pontos = (int) Math.round(PESO_TEMAS * cobertura + PESO_SETOR * setor);
        return new Resultado(Math.min(100, pontos), motivos);
    }

    // 1 = o setor da startup está na lista do mentor; 0,5 = o mentor atende "todas as áreas"; 0 = não
    static double afinidadeDeSetor(String segmento, String setoresDoMentor) {
        if (segmento == null || segmento.isBlank() || setoresDoMentor == null || setoresDoMentor.isBlank()) {
            return 0;
        }
        String alvo = normalizar(segmento);
        Set<String> setores = java.util.Arrays.stream(setoresDoMentor.split("[,;/]"))
                .map(MatchService::normalizar).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        if (setores.contains(alvo)) return 1.0;
        return setores.stream().anyMatch(s -> s.startsWith("todas")) ? 0.5 : 0;
    }

    // Minúsculas, sem acento e sem "s" no fim, para "Health-tech" combinar com "Health-techs"
    static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.endsWith("s") && semAcento.length() > 3
                ? semAcento.substring(0, semAcento.length() - 1) : semAcento;
    }

    private static String juntar(List<String> itens) {
        if (itens.size() == 1) return itens.get(0);
        return String.join(", ", itens.subList(0, itens.size() - 1)) + " e " + itens.get(itens.size() - 1);
    }
}
