package mz.colegiosapi.repo;
import mz.colegiosapi.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<UserAccount,Long> {
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByEncarregadoId(Long id);
    Optional<UserAccount> findByProfessorId(Long id);
    Optional<UserAccount> findByAlunoId(Long id);
    boolean existsByUsername(String username);
}
