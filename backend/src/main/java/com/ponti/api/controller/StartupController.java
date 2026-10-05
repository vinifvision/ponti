package com.ponti.api.controller;

import com.ponti.api.model.Startup;
import com.ponti.api.repository.StartupRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/startups")
public class StartupController {

    private final StartupRepository repository;

    public StartupController(StartupRepository repository) {
        this.repository = repository;
    }

    // READ (todos)
    @GetMapping
    public List<Startup> listar() {
        return repository.findAll();
    }

    // READ (um)
    @GetMapping("/{id}")
    public Startup buscar(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrado(a)"));
    }

    // CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Startup criar(@Valid @RequestBody Startup body) {
        body.setId(null);
        return repository.save(body);
    }

    // UPDATE
    @PutMapping("/{id}")
    public Startup atualizar(@PathVariable Long id, @Valid @RequestBody Startup body) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrado(a)");
        }
        body.setId(id);
        return repository.save(body);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrado(a)");
        }
        repository.deleteById(id);
    }
}
