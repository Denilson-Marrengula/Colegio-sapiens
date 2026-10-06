package mz.colegiosapi.web;

import jakarta.validation.Valid;
import mz.colegiosapi.model.Professor;
import mz.colegiosapi.repo.ProfessorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/professores")
public class ProfessorController {

    private final ProfessorRepository repo;

    public ProfessorController(ProfessorRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA','PROFESSOR')")
    public List<Professor> all() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA','PROFESSOR')")
    public Professor get(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> notFound());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public Professor create(@Valid @RequestBody Professor p) {
        if (p.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não envie id ao criar um registo");
        }
        return repo.save(p);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public Professor update(@PathVariable Long id, @Valid @RequestBody Professor in) {
        Professor p = repo.findById(id).orElseThrow(() -> notFound());
        p.setNome(in.getNome());
        p.setNumeroFuncionario(in.getNumeroFuncionario());
        p.setEmail(in.getEmail());
        p.setTelefone(in.getTelefone());
        return repo.save(p);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        repo.delete(repo.findById(id).orElseThrow(() -> notFound()));
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado");
    }
}
