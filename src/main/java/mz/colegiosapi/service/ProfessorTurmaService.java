package mz.colegiosapi.service;

import mz.colegiosapi.model.Professor;
import mz.colegiosapi.model.Turma;
import mz.colegiosapi.repo.ProfessorRepository;
import mz.colegiosapi.repo.TurmaRepository;
import mz.colegiosapi.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProfessorTurmaService {
    private final ProfessorRepository professors;
    private final TurmaRepository turmas;
    private final UserRepository users;
    public ProfessorTurmaService(ProfessorRepository p,TurmaRepository t,UserRepository u){
        professors=p;turmas=t;users=u;
    }
    public boolean isManagement(Authentication auth){
        return auth.getAuthorities().stream().anyMatch(a -> Set.of(
            "ROLE_ADMIN","ROLE_SECRETARIA","ROLE_DIRECTOR","ROLE_COORDENADOR").contains(a.getAuthority()));
    }
    private Professor find(Long id){
        return professors.findById(id).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,"Professor não encontrado"));
    }
    private Professor ownProfessor(Authentication auth){
        return users.findByUsername(auth.getName()).map(a -> a.getProfessor())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                "A conta não está ligada ao cadastro de professor. Contacte a administração."));
    }
    private List<Turma> sorted(Professor professor){
        return professor.getTurmas().stream().sorted(Comparator.comparing(Turma::getId)).toList();
    }
    @Transactional(readOnly=true)
    public List<Turma> list(Long professorId,Authentication auth){
        if(!isManagement(auth) && !ownProfessor(auth).getId().equals(professorId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Só pode consultar as suas próprias turmas");
        return sorted(find(professorId));
    }
    @Transactional(readOnly=true)
    public List<Turma> own(Authentication auth){return sorted(ownProfessor(auth));}

    @Transactional
    public List<Turma> replace(Long professorId,Set<Long> ids){
        Professor professor=find(professorId);
        List<Turma> found=turmas.findAllById(ids);
        if(found.size()!=ids.size())throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Uma ou mais turmas não existem. Actualize a lista e tente novamente.");
        professor.getTurmas().clear();professor.getTurmas().addAll(new LinkedHashSet<>(found));
        professors.saveAndFlush(professor);
        return sorted(professor);
    }
    @Transactional(readOnly=true)
    public List<Turma> visible(Authentication auth){
        return isManagement(auth)?turmas.findAll():own(auth);
    }
    @Transactional(readOnly=true)
    public Turma visibleById(Long id,Authentication auth){
        if(!isManagement(auth) && own(auth).stream().noneMatch(t -> t.getId().equals(id)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Esta turma não lhe está atribuída");
        return turmas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Turma não encontrada"));
    }
}
