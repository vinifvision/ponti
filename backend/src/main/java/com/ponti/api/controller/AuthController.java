package com.ponti.api.controller;

import com.ponti.api.dto.CadastroMentor;
import com.ponti.api.dto.CadastroStartup;
import com.ponti.api.dto.UsuarioCadastrado;
import com.ponti.api.service.CadastroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final CadastroService cadastroService;

    public AuthController(CadastroService cadastroService) {
        this.cadastroService = cadastroService;
    }

    // Cadastro de startup: cria o usuário e o perfil da startup juntos
    @PostMapping("/cadastro/startup")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioCadastrado cadastrarStartup(@Valid @RequestBody CadastroStartup body) {
        return UsuarioCadastrado.de(cadastroService.cadastrarStartup(body));
    }

    // Cadastro de mentor: cria o usuário e o perfil do mentor juntos
    @PostMapping("/cadastro/mentor")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioCadastrado cadastrarMentor(@Valid @RequestBody CadastroMentor body) {
        return UsuarioCadastrado.de(cadastroService.cadastrarMentor(body));
    }
}
