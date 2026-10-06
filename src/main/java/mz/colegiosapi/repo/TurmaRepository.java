package mz.colegiosapi.repo;

import mz.colegiosapi.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

    boolean existsByCodigoIgnoreCaseAndAnoLetivo(String codigo, Integer anoLetivo);
}
