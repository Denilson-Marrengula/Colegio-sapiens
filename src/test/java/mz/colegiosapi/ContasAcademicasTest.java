package mz.colegiosapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import mz.colegiosapi.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:academicas;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class ContasAcademicasTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Test @WithMockUser(roles="ADMIN")
    void professorAndAlunoReceiveInactiveLinkedAccountsAndDuplicatesDoNotCreateAccounts() throws Exception {
        String prof="{\"nome\":\"Professor Teste\",\"numeroFuncionario\":\"ACA-P1\"}";
        String p=mvc.perform(post("/api/professores").contentType(MediaType.APPLICATION_JSON).content(prof))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long pid=json.readTree(p).get("id").asLong();
        var pu=users.findByProfessorId(pid).orElseThrow();
        assertFalse(pu.isActivo());assertFalse(pu.isCredenciaisDefinidas());
        assertEquals("PROFESSOR",pu.getRole().name());
        long count=users.count();
        mvc.perform(post("/api/professores").contentType(MediaType.APPLICATION_JSON).content(prof))
            .andExpect(status().isConflict());
        assertEquals(count,users.count());
        String turma=mvc.perform(post("/api/turmas").contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigo\":\"ACA-T1\",\"classe\":\"10\",\"anoLetivo\":2026}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long tid=json.readTree(turma).get("id").asLong();
        String aluno="{\"nome\":\"Aluno Teste\",\"numeroMatricula\":\"ACA-A1\",\"turmaId\":"+tid+"}";
        String a=mvc.perform(post("/api/alunos").contentType(MediaType.APPLICATION_JSON).content(aluno))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long aid=json.readTree(a).get("id").asLong();
        var au=users.findByAlunoId(aid).orElseThrow();
        assertFalse(au.isActivo());assertFalse(au.isCredenciaisDefinidas());
        assertEquals("ALUNO",au.getRole().name());
        count=users.count();
        mvc.perform(post("/api/alunos").contentType(MediaType.APPLICATION_JSON).content(aluno))
            .andExpect(status().isConflict());
        assertEquals(count,users.count());
        mvc.perform(patch("/api/auth/utilizadores/{id}/estado",au.getId())
            .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
            .andExpect(status().isConflict());
        mvc.perform(delete("/api/alunos/{id}",aid)).andExpect(status().isConflict());
        mvc.perform(delete("/api/professores/{id}",pid)).andExpect(status().isConflict());
        for(String role:new String[]{"ALUNO","PROFESSOR","ENCARREGADO"}){
            mvc.perform(post("/api/auth/utilizadores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"avulso\",\"nome\":\"Teste\",\"password\":\"Password123!\",\"role\":\""+role+"\"}"))
                .andExpect(status().isBadRequest());
        }
    }
}
