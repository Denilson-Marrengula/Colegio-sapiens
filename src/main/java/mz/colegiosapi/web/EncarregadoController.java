package mz.colegiosapi.web;

import jakarta.validation.Valid;
import mz.colegiosapi.model.Encarregado;
import mz.colegiosapi.model.UserAccount;
import mz.colegiosapi.model.Role;
import mz.colegiosapi.repo.UserRepository;
import mz.colegiosapi.service.ContaAcademicaService;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import mz.colegiosapi.repo.EncarregadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController @RequestMapping("/api/encarregados")
public class EncarregadoController {
    private final EncarregadoRepository repo;
    private final UserRepository users;
    private final ContaAcademicaService contas;
    public EncarregadoController(EncarregadoRepository repo, UserRepository users, ContaAcademicaService contas){
        this.repo=repo; this.users=users; this.contas=contas;
    }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA')") public List<Encarregado> all(){return repo.findAll();}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','COORDENADOR','SECRETARIA')") public Encarregado get(@PathVariable Long id){return repo.findById(id).orElseThrow(()->notFound());}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')") @Transactional public Encarregado create(@Valid @RequestBody Encarregado e){
        if(e.getId()!=null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Não envie id ao criar um registo");
        Encarregado saved=repo.saveAndFlush(e);
        contas.criar(saved);
        return saved;
    }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')") @Transactional public Encarregado update(@PathVariable Long id,@Valid @RequestBody Encarregado in){Encarregado e=repo.findById(id).orElseThrow(()->notFound());e.setNome(in.getNome());e.setDocumento(in.getDocumento());e.setTelefone(in.getTelefone());e.setEmail(in.getEmail());
        users.findByEncarregadoId(id).ifPresent(account -> {
            account.setNome(e.getNome()); account.setEmail(e.getEmail()); account.setTelefone(e.getTelefone());
        });
        return repo.save(e);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") @Transactional public void delete(@PathVariable Long id){
        Encarregado e=repo.findById(id).orElseThrow(()->notFound());
        if(users.findByEncarregadoId(id).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Este encarregado tem uma conta ligada. Desactive a conta para suspender o acesso; a remoção exige tratar as associações.");
        repo.delete(e);
    }
    private ResponseStatusException notFound(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Encarregado não encontrado");}
}
