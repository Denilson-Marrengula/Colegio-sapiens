package mz.colegiosapi.repo;
import mz.colegiosapi.model.Encarregado;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EncarregadoRepository extends JpaRepository<Encarregado,Long> {
    boolean existsByDocumentoIgnoreCase(String documento);
}
