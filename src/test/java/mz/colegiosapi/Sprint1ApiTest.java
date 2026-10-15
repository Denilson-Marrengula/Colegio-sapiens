package mz.colegiosapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:sprint1test;DB_CLOSE_DELAY=-1","app.bootstrap.username=testadmin","app.bootstrap.password=TestAdmin123!"})
@AutoConfigureMockMvc
class Sprint1ApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired mz.colegiosapi.repo.UserRepository users;

    @Test void authAndAcademicSprintFlow() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        mvc.perform(get("/api/turmas")).andExpect(status().isUnauthorized());

        MvcResult turmaResult=mvc.perform(post("/api/turmas").with(httpBasic("testadmin","TestAdmin123!"))
            .contentType(MediaType.APPLICATION_JSON).content("""
              {"codigo":"10A","classe":"10ª Classe","anoLetivo":2026,"turno":"Manhã"}
            """)).andExpect(status().isCreated()).andReturn();
        long turmaId=json.readTree(turmaResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult guardianResult=mvc.perform(post("/api/encarregados").with(httpBasic("testadmin","TestAdmin123!"))
            .contentType(MediaType.APPLICATION_JSON).content("""
              {"nome":"Ana Mucavele","documento":"BI-12345","telefone":"841234567","email":"ana@example.com"}
            """)).andExpect(status().isCreated()).andReturn();
        long guardianId=json.readTree(guardianResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult studentResult=mvc.perform(post("/api/alunos").with(httpBasic("testadmin","TestAdmin123!"))
            .contentType(MediaType.APPLICATION_JSON).content("""
              {"nome":"Amélia Mucavele","numeroMatricula":"MAT-2026-001","dataNascimento":"2010-02-12","turmaId":%d,"encarregadoIds":[]}
            """.formatted(turmaId))).andExpect(status().isCreated()).andReturn();
        long studentId=json.readTree(studentResult.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/alunos/{a}/encarregados/{e}",studentId,guardianId).with(httpBasic("testadmin","TestAdmin123!")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.encarregados[0].id").value(guardianId));

        mvc.perform(put("/api/alunos/{a}/encarregados/{e}",studentId,guardianId).with(httpBasic("testadmin","TestAdmin123!")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.encarregados.length()").value(1));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"testadmin\",\"password\":\"TestAdmin123!\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));

        String professorBody=mvc.perform(post("/api/professores").with(httpBasic("testadmin","TestAdmin123!"))
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"João Professor","numeroFuncionario":"PROF-SPRINT1"}
            """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long pid=json.readTree(professorBody).get("id").asLong();
        long uid=users.findByProfessorId(pid).orElseThrow().getId();
        mvc.perform(put("/api/auth/utilizadores/{id}",uid).with(httpBasic("testadmin","TestAdmin123!"))
            .contentType(MediaType.APPLICATION_JSON).content("""
             {"username":"professor1","nome":"João Professor","password":"SenhaSegura123","role":"PROFESSOR"}
            """)).andExpect(status().isOk()).andExpect(jsonPath("$.professorId").value(pid));
        mvc.perform(patch("/api/auth/utilizadores/{id}/estado",uid).with(httpBasic("testadmin","TestAdmin123!"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}")).andExpect(status().isOk());

        mvc.perform(post("/api/turmas").with(httpBasic("professor1","SenhaSegura123"))
            .contentType(MediaType.APPLICATION_JSON).content("""
              {"codigo":"10B","classe":"10ª Classe","anoLetivo":2026,"turno":"Tarde"}
            """)).andExpect(status().isForbidden());
        mvc.perform(get("/api/alunos").with(httpBasic("professor1","SenhaSegura123")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/encarregados").with(httpBasic("professor1","SenhaSegura123")))
            .andExpect(status().isForbidden());
    }
}
