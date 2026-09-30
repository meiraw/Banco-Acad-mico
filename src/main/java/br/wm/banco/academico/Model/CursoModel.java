package br.wm.banco.academico.Model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
@Table(name = "cursos")
public class CursoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Column(nullable = false, unique = true)
    private String nome;

    @Setter
    @OneToMany(mappedBy = "curso")
    private List<SemestreModel> semestres;
}
