package com.ponti.api.controller;

import com.ponti.api.model.Tema;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Lista de temas para os formulários (cadastro de startup, cadastro de mentor, filtros).
// É pública porque o cadastro acontece antes do login.
@RestController
@RequestMapping("/temas")
public class TemaController {

    public record TemaResposta(String codigo, String nome) {
    }

    @GetMapping
    public List<TemaResposta> listar() {
        return Arrays.stream(Tema.values()).map(t -> new TemaResposta(t.name(), t.getRotulo())).toList();
    }
}
