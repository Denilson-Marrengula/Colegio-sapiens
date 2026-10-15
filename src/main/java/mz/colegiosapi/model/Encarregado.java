package mz.colegiosapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "encarregados", uniqueConstraints = @UniqueConstraint(columnNames = "documento"))
public class Encarregado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;
    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String documento;
    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String telefone;
    @Email
    @Size(max = 160)
    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
