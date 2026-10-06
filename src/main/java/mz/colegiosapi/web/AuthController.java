package mz.colegiosapi.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import mz.colegiosapi.model.Role;
import mz.colegiosapi.model.UserAccount;
import mz.colegiosapi.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(AuthenticationManager a, UserRepository u, PasswordEncoder e) {
        authManager = a;
        users = u;
        encoder = e;
    }

    @PostMapping("/login")
    public AccountView login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        return AccountView.from(users.findByUsername(auth.getName()).orElseThrow());
    }

    @GetMapping("/utilizadores")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public List<AccountView> all() {
        return users.findAll().stream().map(AccountView::from).toList();
    }

    @PostMapping("/utilizadores")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView create(@Valid @RequestBody CreateUserRequest request, Authentication auth) {
        requireAssignableRole(request.role(), auth);
        if (users.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome de utilizador já existe");
        }
        UserAccount account = new UserAccount();
        account.setUsername(request.username());
        account.setNome(request.nome());
        account.setEmail(request.email());
        account.setTelefone(request.telefone());
        account.setPasswordHash(encoder.encode(request.password()));
        account.setRole(request.role());
        account.setActivo(true);
        return AccountView.from(users.save(account));
    }

    @PutMapping("/utilizadores/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    @Transactional
    public AccountView update(@PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request, Authentication auth) {
        UserAccount account = find(id);
        requireAssignableRole(account.getRole(), auth);
        requireAssignableRole(request.role(), auth);
        if (account.getUsername().equals(auth.getName()) && request.role() != account.getRole()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não pode alterar o seu próprio perfil");
        }
        users.findByUsername(request.username()).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome de utilizador já existe");
                });
        if (request.password() != null && !request.password().isEmpty()) {
            if (request.password().isBlank() || request.password().length() < 8 || request.password().length() > 72) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A palavra-passe deve ter entre 8 e 72 caracteres");
            }
            account.setPasswordHash(encoder.encode(request.password()));
        }
        if (account.getUsername().equals(auth.getName()) && !account.getUsername().equals(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não pode alterar o seu próprio nome de utilizador");
        }
        account.setUsername(request.username());
        account.setNome(request.nome());
        account.setEmail(request.email());
        account.setTelefone(request.telefone());
        account.setRole(request.role());
        return AccountView.from(users.save(account));
    }

    @PatchMapping("/utilizadores/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AccountView state(@PathVariable Long id, @Valid @RequestBody StateRequest request,
            Authentication auth) {
        UserAccount account = find(id);
        if (!request.activo() && account.getUsername().equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não pode desactivar a sua própria conta");
        }
        account.setActivo(request.activo());
        return AccountView.from(users.save(account));
    }

    private UserAccount find(Long id) {
        return users.findById(id).orElseThrow(()
                -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
    }

    private void requireAssignableRole(Role role, Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && (role == Role.ADMIN || role == Role.DIRECTOR || role == Role.COORDENADOR || role == Role.SECRETARIA)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas administradores podem gerir contas com privilégios de gestão");
        }
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record CreateUserRequest(@NotBlank
            @Size(max = 60)
            @Pattern(regexp = "[^:]+", message = "não pode conter dois-pontos") String username,
            @NotBlank
            @Size(max = 120) String nome, @NotBlank
            @Size(min = 8, max = 72) String password,
            @NotNull Role role, @Email
            @Size(max = 160) String email, @Size(max = 30) String telefone) {}

    public record UpdateUserRequest(@NotBlank
            @Size(max = 60)
            @Pattern(regexp = "[^:]+", message = "não pode conter dois-pontos") String username,
            @NotBlank
            @Size(max = 120) String nome, String password, @NotNull Role role,
            @Email
            @Size(max = 160) String email, @Size(max = 30) String telefone) {}

    public record StateRequest(@NotNull Boolean activo) {}

    public record AccountView(Long id, String username, String nome, Role role, boolean activo,
            String email, String telefone) {

        static AccountView from(UserAccount a) {
            return new AccountView(a.getId(), a.getUsername(), a.getNome(), a.getRole(),
                    a.isActivo(), a.getEmail(), a.getTelefone());
        }
    }
}
