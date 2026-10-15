package mz.colegiosapi.service;

import mz.colegiosapi.model.*;
import mz.colegiosapi.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.UUID;

@Service
@Transactional(propagation=Propagation.MANDATORY)
public class ContaAcademicaService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    public ContaAcademicaService(UserRepository users, PasswordEncoder encoder){
        this.users=users; this.encoder=encoder;
    }
    private UserAccount nova(Role role,String nome,String email,String telefone){
        UserAccount a=new UserAccount();
        a.setUsername(role.name().toLowerCase(java.util.Locale.ROOT)+"-"+UUID.randomUUID());
        a.setNome(nome); a.setEmail(email); a.setTelefone(telefone);
        a.setRole(role); a.setActivo(false); a.setCredenciaisDefinidas(false);
        a.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));
        return a;
    }
    public void criar(Encarregado e){
        UserAccount a=nova(Role.ENCARREGADO,e.getNome(),e.getEmail(),e.getTelefone());
        a.setEncarregado(e); users.saveAndFlush(a);
    }
    public void criar(Professor p){
        UserAccount a=nova(Role.PROFESSOR,p.getNome(),p.getEmail(),p.getTelefone());
        a.setProfessor(p); users.saveAndFlush(a);
    }
    public void criar(Aluno aluno){
        UserAccount a=nova(Role.ALUNO,aluno.getNome(),null,null);
        a.setAluno(aluno); users.saveAndFlush(a);
    }
}
