package com.ponti.api.controller;

import com.ponti.api.model.Mentor;
import com.ponti.api.repository.MentorRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/mentores")
public class MentorController {

    private final MentorRepository repository;

    public MentorController(MentorRepository repository) {
        this.repository = repository;
    }

    // READ (todos)
    @GetMapping
    public List<Mentor> listar() {
        return repository.findAll();
    }

    // READ (um)
    @GetMapping("/{id}")
    public Mentor buscar(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado(a)"));
    }

    // CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mentor criar(@Valid @RequestBody Mentor body) {
        body.setId(null);
        return repository.save(body);
    }

    // UPDATE
    @PutMapping("/{id}")
    public Mentor atualizar(@PathVariable Long id, @Valid @RequestBody Mentor body) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado(a)");
        }
        body.setId(id);
        return repository.save(body);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor não encontrado(a)");
        }
        repository.deleteById(id);
    }
}
