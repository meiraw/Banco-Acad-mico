package br.wm.banco.academico.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Setter
    @Column(nullable = false)
    private Integer semestre;


    public DisciplinasModel(String nome, String descricao, Integer semestre){
        this.nome = nome;
        this.descricao = descricao;
        this.semestre = semestre;
    }
}
