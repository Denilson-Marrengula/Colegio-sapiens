package mz.colegiosapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "professores", uniqueConstraints = @UniqueConstraint(columnNames = "numero_funcionario"))
public class Professor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;
    @NotBlank
    @Size(max = 30)
    @Column(name = "numero_funcionario", nullable = false, length = 30)
    private String numeroFuncionario;
    @Email
    @Size(max = 160)
    private String email;
    @Size(max = 30)
    private String telefone;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String v) {
        nome = v;
    }

    public String getNumeroFuncionario() {
        return numeroFuncionario;
    }

    public void setNumeroFuncionario(String v) {
        numeroFuncionario = v;
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
}
