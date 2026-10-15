package mz.colegiosapi.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import mz.colegiosapi.model.Turma;
import mz.colegiosapi.service.ProfessorTurmaService;
import org.springframework.security.core.Authentication;
import java.util.Set;
import mz.colegiosapi.model.Professor;
import mz.colegiosapi.repo.ProfessorRepository;
import mz.colegiosapi.repo.UserRepository;
import mz.colegiosapi.service.ContaAcademicaService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController @RequestMapping("/api/professores")
public class ProfessorController {
    private final ProfessorRepository repo;
    private final UserRepository users;
    private final ContaAcademicaService contas;
    private final ProfessorTurmaService atribuicoes;
    public ProfessorController(ProfessorRepository repo, UserRepository users, ContaAcademicaService contas, ProfessorTurmaService atribuicoes){
        this.repo=repo;this.users=users;this.contas=contas;this.atribuicoes=atribuicoes;
    }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA','PROFESSOR')") public List<Professor> all(){return repo.findAll();}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA','PROFESSOR')") public Professor get(@PathVariable Long id){return repo.findById(id).orElseThrow(()->notFound());}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')") @Transactional public Professor create(@Valid @RequestBody Professor p){if (p.getId() != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Não envie id ao criar um registo");Professor saved=repo.saveAndFlush(p);contas.criar(saved);return saved;}
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')") @Transactional public Professor update(@PathVariable Long id,@Valid @RequestBody Professor in){Professor p=repo.findById(id).orElseThrow(()->notFound());p.setNome(in.getNome());p.setNumeroFuncionario(in.getNumeroFuncionario());p.setEmail(in.getEmail());p.setTelefone(in.getTelefone());
        users.findByProfessorId(id).ifPresent(a->{
            a.setNome(p.getNome());a.setEmail(p.getEmail());a.setTelefone(p.getTelefone());
        });return repo.save(p);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") @Transactional public void delete(@PathVariable Long id){
        Professor p=repo.findById(id).orElseThrow(()->notFound());
        if(users.findByProfessorId(id).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Professor com conta ligada: desactive a conta para suspender o acesso");
        repo.delete(p);
    }
    @GetMapping("/minhas-turmas") @PreAuthorize("hasRole('PROFESSOR')")
    public List<Turma> ownClasses(Authentication auth){return atribuicoes.own(auth);}
    @GetMapping("/{id}/turmas")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA','PROFESSOR')")
    public List<Turma> classes(@PathVariable Long id,Authentication auth){return atribuicoes.list(id,auth);}
    @PutMapping("/{id}/turmas") @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public List<Turma> assign(@PathVariable Long id,@Valid @RequestBody TurmasRequest input){
        return atribuicoes.replace(id,input.turmaIds());
    }
    public record TurmasRequest(@NotNull Set<@NotNull Long> turmaIds){}
    private ResponseStatusException notFound(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Professor não encontrado");}
}
