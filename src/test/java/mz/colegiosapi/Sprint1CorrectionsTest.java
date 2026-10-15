package mz.colegiosapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:corrections;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class Sprint1CorrectionsTest {
    @Autowired MockMvc mvc;

    @Test @WithMockUser(roles="SECRETARIA")
    void secretariaCannotCreateManagementAccounts() throws Exception {
        for (String role : new String[]{"ADMIN","DIRECTOR","COORDENADOR","SECRETARIA"}) {
            mvc.perform(post("/api/auth/utilizadores").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"blocked","nome":"Teste","password":"Password123!","role":"%s"}
                    """.formatted(role))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/auth/utilizadores").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"allowed-professor","nome":"Professor","password":"Password123!","role":"PROFESSOR"}
                """)).andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles="ADMIN")
    void createRejectsClientProvidedIds() throws Exception {
        mvc.perform(post("/api/turmas").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"id":1,"codigo":"TEST","classe":"10","anoLetivo":2026}
                """)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/professores").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"id":1,"nome":"Teste","numeroFuncionario":"F-1"}
                """)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/encarregados").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"id":1,"nome":"Teste","documento":"BI-1","telefone":"841234567"}
                """)).andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles="ADMIN")
    void futureBirthDateIsRejected() throws Exception {
        mvc.perform(post("/api/alunos").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"nome":"Teste","numeroMatricula":"M-1","dataNascimento":"2999-01-01","turmaId":1}
                """)).andExpect(status().isBadRequest());
    }
}
