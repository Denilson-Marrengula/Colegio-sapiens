package mz.colegiosapi.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

@Entity
@Table(name = "utilizadores", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 60)
    private String username;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(length = 160)
    private String email;
    @Column(length = 30)
    private String telefone;
    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    @Column(nullable = false)
    private boolean activo = true;
    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        username = v;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String v) {
        nome = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        email = v;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String v) {
        telefone = v;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String v) {
        passwordHash = v;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role v) {
        role = v;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean v) {
        activo = v;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
