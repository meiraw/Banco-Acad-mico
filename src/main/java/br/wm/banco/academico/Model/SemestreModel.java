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
    private Integer  ano;

    @Setter
    @Column(nullable = false)
    private Integer  periodo;

    @Setter
    @OneToMany(mappedBy = "Semestre")
    private List<DisciplinasModel> disciplina ;

}
