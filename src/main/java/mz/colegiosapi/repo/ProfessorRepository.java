package mz.colegiosapi.repo;
import mz.colegiosapi.model.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProfessorRepository extends JpaRepository<Professor,Long> {
    boolean existsByNumeroFuncionarioIgnoreCase(String numeroFuncionario);
}
