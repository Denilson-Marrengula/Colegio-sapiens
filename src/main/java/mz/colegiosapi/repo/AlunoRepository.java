package mz.colegiosapi.repo;
import mz.colegiosapi.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AlunoRepository extends JpaRepository<Aluno,Long> {
    boolean existsByNumeroMatriculaIgnoreCase(String numeroMatricula);
}
