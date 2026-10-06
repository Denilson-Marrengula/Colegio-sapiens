package mz.colegiosapi.web;

import jakarta.validation.Valid;
import mz.colegiosapi.model.Encarregado;
import mz.colegiosapi.repo.EncarregadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/encarregados")
public class EncarregadoController {

    private final EncarregadoRepository repo;

    public EncarregadoController(EncarregadoRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA')")
    public List<Encarregado> all() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA')")
    public Encarregado get(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> notFound());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public Encarregado create(@Valid @RequestBody Encarregado e) {
        if (e.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não envie id ao criar um registo");
        }
        return repo.save(e);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public Encarregado update(@PathVariable Long id, @Valid @RequestBody Encarregado in) {
        Encarregado e = repo.findById(id).orElseThrow(() -> notFound());
        e.setNome(in.getNome());
        e.setDocumento(in.getDocumento());
        e.setTelefone(in.getTelefone());
        e.setEmail(in.getEmail());
        return repo.save(e);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        repo.delete(repo.findById(id).orElseThrow(() -> notFound()));
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Encarregado não encontrado");
    }
}
