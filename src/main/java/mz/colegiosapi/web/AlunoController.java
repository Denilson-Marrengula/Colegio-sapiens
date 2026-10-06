package mz.colegiosapi.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import mz.colegiosapi.model.*;
import mz.colegiosapi.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/alunos")
public class AlunoController {

    private final AlunoRepository alunos;
    private final TurmaRepository turmas;
    private final EncarregadoRepository encarregados;

    public AlunoController(AlunoRepository a, TurmaRepository t, EncarregadoRepository e) {
        alunos = a;
        turmas = t;
        encarregados = e;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA')")
    public List<Aluno> all() {
        return alunos.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA')")
    public Aluno get(@PathVariable Long id) {
        return findAluno(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    @Transactional
    public Aluno create(@Valid @RequestBody AlunoRequest req) {
        Aluno a = new Aluno();
        apply(a, req);
        return alunos.save(a);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    @Transactional
    public Aluno update(@PathVariable Long id, @Valid @RequestBody AlunoRequest req) {
        Aluno a = findAluno(id);
        apply(a, req);
        return alunos.save(a);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        alunos.delete(findAluno(id));
    }

    @PutMapping("/{alunoId}/encarregados/{encarregadoId}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    @Transactional
    public Aluno associate(@PathVariable Long alunoId, @PathVariable Long encarregadoId) {
        Aluno a = findAluno(alunoId);
        Encarregado e = encarregados.findById(encarregadoId).orElseThrow(() -> notFound("Encarregado não encontrado"));
        if (a.getEncarregados().stream().noneMatch(existing -> existing.getId().equals(encarregadoId))) {
            a.getEncarregados().add(e);
        }
        return alunos.save(a);
    }

    @DeleteMapping("/{alunoId}/encarregados/{encarregadoId}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    @Transactional
    public Aluno disassociate(@PathVariable Long alunoId, @PathVariable Long encarregadoId) {
        Aluno a = findAluno(alunoId);
        a.getEncarregados().removeIf(e -> e.getId().equals(encarregadoId));
        return alunos.save(a);
    }

    private void apply(Aluno a, AlunoRequest req) {
        a.setNome(req.nome());
        a.setNumeroMatricula(req.numeroMatricula());
        a.setDataNascimento(req.dataNascimento());
        a.setTurma(turmas.findById(req.turmaId()).orElseThrow(() -> notFound("Turma não encontrada")));
        Set<Long> ids = req.encarregadoIds() == null ? Set.of() : req.encarregadoIds();
        List<Encarregado> found = encarregados.findAllById(ids);
        if (found.size() != ids.size()) {
            throw notFound("Um ou mais encarregados não foram encontrados");
        }
        a.setEncarregados(new LinkedHashSet<>(found));
    }

    private Aluno findAluno(Long id) {
        return alunos.findById(id).orElseThrow(() -> notFound("Aluno não encontrado"));
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    public record AlunoRequest(@NotBlank
            @Size(max = 120) String nome, @NotBlank
            @Size(max = 30) String numeroMatricula,
            @PastOrPresent LocalDate dataNascimento, @NotNull
            @Positive Long turmaId, Set<@NotNull @Positive Long> encarregadoIds) {}
}
