package mz.colegiosapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "turmas", uniqueConstraints = @UniqueConstraint(columnNames = {"codigo", "ano_letivo"}))
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String codigo;
    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String classe;
    @NotNull
    @Min(2020)
    @Max(2100)
    @Column(name = "ano_letivo", nullable = false)
    private Integer anoLetivo;
    @Size(max = 30)
    private String turno;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String v) {
        codigo = v;
    }

    public String getClasse() {
        return classe;
    }

    public void setClasse(String v) {
        classe = v;
    }

    public Integer getAnoLetivo() {
        return anoLetivo;
    }

    public void setAnoLetivo(Integer v) {
        anoLetivo = v;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String v) {
        turno = v;
    }
}
