package mz.colegiosapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "alunos", uniqueConstraints = @UniqueConstraint(columnNames = "numero_matricula"))
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;
    @NotBlank
    @Size(max = 30)
    @Column(name = "numero_matricula", nullable = false, length = 30)
    private String numeroMatricula;
    @PastOrPresent
    private LocalDate dataNascimento;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "turma_id")
    private Turma turma;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "aluno_encarregado", joinColumns = @JoinColumn(name = "aluno_id"), inverseJoinColumns = @JoinColumn(name = "encarregado_id"))
    private Set<Encarregado> encarregados = new LinkedHashSet<>();

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

    public String getNumeroMatricula() {
        return numeroMatricula;
    }

    public void setNumeroMatricula(String v) {
        numeroMatricula = v;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate v) {
        dataNascimento = v;
    }

    public Turma getTurma() {
        return turma;
    }

    public void setTurma(Turma v) {
        turma = v;
    }

    public Set<Encarregado> getEncarregados() {
        return encarregados;
    }

    public void setEncarregados(Set<Encarregado> v) {
        encarregados = v;
    }
}
