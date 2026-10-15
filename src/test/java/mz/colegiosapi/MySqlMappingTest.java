package mz.colegiosapi;

import jakarta.persistence.EntityManager;
import mz.colegiosapi.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

// Executar apenas contra uma base de testes MySQL, com as tabelas do script SQL.
@SpringBootTest(properties={
    "spring.datasource.url=${MYSQL_TEST_URL}",
    "spring.datasource.username=${MYSQL_TEST_USER:root}",
    "spring.datasource.password=${MYSQL_TEST_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
})
@EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS",matches="true")
@Transactional
class MySqlMappingTest {
    @Autowired EntityManager em;

    @Test void mysqlPersistsEntitiesEnumDatesAndRelationships() {
        String suffix=java.util.UUID.randomUUID().toString().substring(0,8);
        Turma turma=new Turma();
        turma.setCodigo("TEST-"+suffix);turma.setClasse("10ª Classe");turma.setAnoLetivo(2026);
        em.persist(turma);
        Encarregado guardian=new Encarregado();
        guardian.setNome("Ana Mucavele");guardian.setDocumento("TEST-"+suffix);guardian.setTelefone("841234567");
        em.persist(guardian);
        Professor teacher=new Professor();
        teacher.setNome("Professor Teste");teacher.setNumeroFuncionario("TEST-"+suffix);
        teacher.getTurmas().add(turma);
        em.persist(teacher);
        Aluno student=new Aluno();
        student.setNome("Amélia Teste");student.setNumeroMatricula("TEST-"+suffix);
        student.setDataNascimento(LocalDate.of(2010,2,12));student.setTurma(turma);
        student.getEncarregados().add(guardian);em.persist(student);
        UserAccount user=new UserAccount();
        user.setUsername("test-"+suffix);user.setNome("Conta de teste");user.setRole(Role.PROFESSOR);
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Teste123!"));
        user.setActivo(false);user.setCredenciaisDefinidas(false);
        user.setProfessor(teacher);em.persist(user);
        em.flush();em.clear();
        Aluno saved=em.find(Aluno.class,student.getId());
        assertEquals("Amélia Teste",saved.getNome());
        assertEquals(LocalDate.of(2010,2,12),saved.getDataNascimento());
        assertEquals(turma.getId(),saved.getTurma().getId());
        assertEquals(guardian.getId(),saved.getEncarregados().iterator().next().getId());
        UserAccount savedUser=em.find(UserAccount.class,user.getId());
        assertEquals(Role.PROFESSOR,savedUser.getRole());assertFalse(savedUser.isActivo());
        assertNotNull(savedUser.getCriadoEm());
        assertFalse(savedUser.isCredenciaisDefinidas());
        assertEquals(teacher.getId(),savedUser.getProfessor().getId());
        assertEquals(turma.getId(),em.find(Professor.class,teacher.getId()).getTurmas().iterator().next().getId());
        assertNotNull(em.find(Professor.class,teacher.getId()));
    }
}
