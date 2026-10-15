package mz.colegiosapi;

import mz.colegiosapi.model.*;
import mz.colegiosapi.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:professorturmas;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc @Transactional @WithMockUser(roles="ADMIN")
class ProfessorTurmaTest {
    @Autowired MockMvc mvc;
    @Autowired ProfessorRepository professors;
    @Autowired TurmaRepository turmas;
    @Autowired UserRepository users;
    Professor p1,p2;
    Turma t1,t2,t3;
    String username;

    @BeforeEach void fixtures(){
        String suffix=UUID.randomUUID().toString().substring(0,8);username="prof-"+suffix;
        p1=professor("P1-"+suffix);p2=professor("P2-"+suffix);
        t1=turma("A-"+suffix);t2=turma("B-"+suffix);t3=turma("C-"+suffix);
        UserAccount account=new UserAccount();account.setUsername(username);account.setNome("Professor Teste");
        account.setPasswordHash("hash-de-teste");account.setRole(Role.PROFESSOR);account.setProfessor(p1);
        users.saveAndFlush(account);
    }
    private Professor professor(String number){
        Professor p=new Professor();p.setNome("Professor "+number);p.setNumeroFuncionario(number);
        return professors.saveAndFlush(p);
    }
    private Turma turma(String code){
        Turma t=new Turma();t.setCodigo(code);t.setClasse("10ª Classe");t.setAnoLetivo(2026);
        return turmas.saveAndFlush(t);
    }
    private void assign(Professor p,String ids) throws Exception {
        mvc.perform(put("/api/professores/{id}/turmas",p.getId()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"turmaIds\":["+ids+"]}")).andExpect(status().isOk());
    }
    @Test void manyToManyAndTeacherAccessAreScoped() throws Exception {
        assign(p1,t1.getId()+","+t2.getId());assign(p2,t1.getId()+","+t3.getId());
        mvc.perform(get("/api/professores/{id}/turmas",p2.getId())).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/professores/minhas-turmas").with(user(username).roles("PROFESSOR")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(t1.getId())).andExpect(jsonPath("$[1].id").value(t2.getId()));
        mvc.perform(get("/api/turmas").with(user(username).roles("PROFESSOR")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/turmas/{id}",t3.getId()).with(user(username).roles("PROFESSOR")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/professores/{id}/turmas",p2.getId()).with(user(username).roles("PROFESSOR")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/professores/{id}/turmas",p1.getId()).with(user(username).roles("PROFESSOR")))
            .andExpect(status().isOk());
        mvc.perform(get("/api/professores/{id}",p1.getId())).andExpect(jsonPath("$.turmas").doesNotExist());
    }
    @Test void invalidAssignmentPreservesExistingAndEmptyListRemovesLinks() throws Exception {
        assign(p1,t1.getId().toString());
        mvc.perform(put("/api/professores/{id}/turmas",p1.getId()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"turmaIds\":["+t2.getId()+",9223372036854775807]}"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/professores/{id}/turmas",p1.getId())).andExpect(jsonPath("$[0].id").value(t1.getId()))
            .andExpect(jsonPath("$.length()").value(1));
        assign(p1,t2.getId()+","+t2.getId());
        mvc.perform(get("/api/professores/{id}/turmas",p1.getId())).andExpect(jsonPath("$.length()").value(1));
        assign(p1,"");
        mvc.perform(get("/api/professores/minhas-turmas").with(user(username).roles("PROFESSOR")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/professores/{id}/turmas",p1.getId()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"turmaIds\":[null]}")) .andExpect(status().isBadRequest());
    }
    @Test void onlyAdminAndSecretariaCanAssign() throws Exception {
        mvc.perform(put("/api/professores/{id}/turmas",p1.getId()).with(user("secretaria").roles("SECRETARIA"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"turmaIds\":["+t1.getId()+"]}"))
            .andExpect(status().isOk());
        for(String role:new String[]{"DIRECTOR","COORDENADOR","PROFESSOR","ALUNO","ENCARREGADO"}){
            mvc.perform(put("/api/professores/{id}/turmas",p1.getId()).with(user(username).roles(role))
                .contentType(MediaType.APPLICATION_JSON).content("{\"turmaIds\":[]}"))
                .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/professores/{id}/turmas",p1.getId()).with(user("director").roles("DIRECTOR")))
            .andExpect(status().isOk());
        mvc.perform(get("/api/professores/minhas-turmas").with(anonymous())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/professores/minhas-turmas").with(user("legacy-prof").roles("PROFESSOR")))
            .andExpect(status().isConflict());
    }
}
