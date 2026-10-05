package com.ponti.api.controller;

import com.ponti.api.model.Convite;
import com.ponti.api.repository.ConviteRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/convites")
public class ConviteController {

    private final ConviteRepository repository;

    public ConviteController(ConviteRepository repository) {
        this.repository = repository;
    }

    // READ (todos)
    @GetMapping
    public List<Convite> listar() {
        return repository.findAll();
    }

    // READ (um)
    @GetMapping("/{id}")
    public Convite buscar(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado(a)"));
    }

    // CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Convite criar(@Valid @RequestBody Convite body) {
        body.setId(null);
        return repository.save(body);
    }

    // UPDATE
    @PutMapping("/{id}")
    public Convite atualizar(@PathVariable Long id, @Valid @RequestBody Convite body) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado(a)");
        }
        body.setId(id);
        return repository.save(body);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado(a)");
        }
        repository.deleteById(id);
    }
}
