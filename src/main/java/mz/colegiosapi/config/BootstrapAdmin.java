package mz.colegiosapi.config;

import mz.colegiosapi.model.Role;
import mz.colegiosapi.model.UserAccount;
import mz.colegiosapi.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdmin {
    private static final String[] ADMIN_USERNAMES = { "jemisse1st@gmail.com", "Admin@gmail.com" };

    @Bean
    CommandLineRunner createInitialAdmins(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            for (String username : ADMIN_USERNAMES) {
                if (users.existsByUsername(username))
                    continue;
                UserAccount admin = new UserAccount();
                admin.setUsername(username);
                admin.setNome("Administrador do Sistema");
                admin.setRole(Role.ADMIN);
                admin.setPasswordHash(encoder.encode(username));
                admin.setActivo(true);
                users.save(admin);
            }
        };
    }
}
