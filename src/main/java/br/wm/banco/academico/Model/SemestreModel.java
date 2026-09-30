package br.wm.banco.academico.Model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;


@Entity
@Getter
@NoArgsConstructor
@Table(name = "semestres")

public class SemestreModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Column(nullable = false)
    private String nome;

    @Setter
    @Column(nullable = false)
    private Integer  numero;

    @Setter
    @OneToMany(mappedBy = "semestre")
    private List<DisciplinasModel> disciplinas;

    @Setter
    @ManyToOne
    @JoinColumn(name = "curso_id", nullable = false )
    private CursoModel curso;
}
