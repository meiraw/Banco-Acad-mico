package br.wm.banco.academico.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "disciplinas") //nome da tabela
public class DisciplinasModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) //Informa que o banco gera o ID automaticamente.
    private UUID id;

    @Setter
    @Column(nullable = false)
    private String nome;

    @Setter
    @Column(nullable = false)
    private String descricao;


    public DisciplinasModel(String nome, String descricao){
        this.nome = nome;
        this.descricao = descricao;
    }

    @Setter
    @OneToMany(mappedBy = "disciplina")
    private List<MaterialModel> material = new ArrayList<>();

    @Setter
    @ManyToOne
    @JoinColumn(name = "semestre_id", nullable = false)
    private SemestreModel semestre;

}
