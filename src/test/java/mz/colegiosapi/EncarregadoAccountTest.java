package mz.colegiosapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import mz.colegiosapi.repo.UserRepository;
import mz.colegiosapi.repo.EncarregadoRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:encaccounts;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class EncarregadoAccountTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired EncarregadoRepository encarregados;

    @Test @WithMockUser(username="admin",roles="ADMIN")
    void createsLinkedInactiveAccountAndRequiresPasswordBeforeActivation() throws Exception {
        String input="""
            {"nome":"Encarregado Teste","documento":"DOC-LINK-01","telefone":"841234567","email":"pai@example.com"}
            """;
        String body=mvc.perform(post("/api/encarregados").contentType(MediaType.APPLICATION_JSON).content(input))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long eid=json.readTree(body).get("id").asLong();
        var account=users.findByEncarregadoId(eid).orElseThrow();
        long uid=account.getId();
        assertFalse(account.isActivo());
        assertFalse(account.isCredenciaisDefinidas());
        assertEquals("ENCARREGADO",account.getRole().name());
        long count=users.count();
        mvc.perform(post("/api/encarregados").contentType(MediaType.APPLICATION_JSON).content(input))
            .andExpect(status().isConflict());
        assertEquals(count,users.count());
        mvc.perform(patch("/api/auth/utilizadores/{id}/estado",uid)
            .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
            .andExpect(status().isConflict());
        String update=json.writeValueAsString(java.util.Map.of(
            "username","pai-link","nome","Nome Editado","role","ENCARREGADO",
            "telefone","841234568","password","Inicial123!"));
        mvc.perform(put("/api/auth/utilizadores/{id}",uid).contentType(MediaType.APPLICATION_JSON).content(update))
            .andExpect(status().isOk()).andExpect(jsonPath("$.encarregadoId").value(eid))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertEquals("Nome Editado",encarregados.findById(eid).orElseThrow().getNome());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"pai-link\",\"password\":\"Inicial123!\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/auth/utilizadores/{id}/estado",uid)
            .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"pai-link\",\"password\":\"Inicial123!\"}"))
            .andExpect(status().isOk());
        mvc.perform(put("/api/auth/password").with(httpBasic("pai-link","Inicial123!"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"currentPassword\":\"errada\",\"newPassword\":\"NovaSenha123!\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/auth/password").with(httpBasic("pai-link","Inicial123!"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"currentPassword\":\"Inicial123!\",\"newPassword\":\"NovaSenha123!\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"pai-link\",\"password\":\"Inicial123!\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"pai-link\",\"password\":\"NovaSenha123!\"}"))
            .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles="SECRETARIA")
    void secretariaCanRegisterButCannotConfigureLinkedCredentials() throws Exception {
        String body=mvc.perform(post("/api/encarregados").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Segundo Pai\",\"documento\":\"DOC-LINK-02\",\"telefone\":\"841234567\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long eid=json.readTree(body).get("id").asLong();
        long uid=users.findByEncarregadoId(eid).orElseThrow().getId();
        mvc.perform(put("/api/auth/utilizadores/{id}",uid).contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"segundo-pai\",\"nome\":\"Segundo Pai\",\"role\":\"ENCARREGADO\",\"telefone\":\"841234567\",\"password\":\"Inicial123!\"}"))
            .andExpect(status().isForbidden());
    }
}
