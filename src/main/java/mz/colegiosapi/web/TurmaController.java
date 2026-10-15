package mz.colegiosapi.web;

import jakarta.validation.Valid;
import mz.colegiosapi.service.ProfessorTurmaService;
import org.springframework.security.core.Authentication;
import mz.colegiosapi.model.Turma;
import mz.colegiosapi.repo.TurmaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController @RequestMapping("/api/turmas")
public class TurmaController {
    private final TurmaRepository repo;
    private final ProfessorTurmaService atribuicoes;
    public TurmaController(TurmaRepository repo,ProfessorTurmaService atribuicoes){this.repo=repo;this.atribuicoes=atribuicoes;}
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA','PROFESSOR')") public List<Turma> all(Authentication auth){return atribuicoes.visible(auth);}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA','PROFESSOR')") public Turma get(@PathVariable Long id,Authentication auth){return atribuicoes.visibleById(id,auth);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')") public Turma create(@Valid @RequestBody Turma t){if (t.getId() != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Não envie id ao criar um registo");return repo.save(t);}
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')") public Turma update(@PathVariable Long id,@Valid @RequestBody Turma input){Turma t=repo.findById(id).orElseThrow(()->notFound());t.setCodigo(input.getCodigo());t.setClasse(input.getClasse());t.setAnoLetivo(input.getAnoLetivo());t.setTurno(input.getTurno());return repo.save(t);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") public void delete(@PathVariable Long id){repo.delete(repo.findById(id).orElseThrow(()->notFound()));}
    private ResponseStatusException notFound(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Turma não encontrada");}
}
