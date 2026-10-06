package mz.colegiosapi.config;

import mz.colegiosapi.model.Role;
import mz.colegiosapi.model.UserAccount;
import mz.colegiosapi.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdmin {

    @Bean
    CommandLineRunner createInitialAdmin(UserRepository users, PasswordEncoder encoder,
            @Value("${app.bootstrap.username}") String username,
            @Value("${app.bootstrap.password}") String password,
            @Value("${app.bootstrap.name}") String name) {
        return args -> {
            if (!users.existsByUsername(username)) {
                UserAccount admin = new UserAccount();
                admin.setUsername(username);
                admin.setNome(name);
                admin.setRole(Role.ADMIN);
                admin.setPasswordHash(encoder.encode(password));
                admin.setActivo(true);
                users.save(admin);
            }
        };
    }
}
