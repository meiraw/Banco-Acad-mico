package br.wm.banco.academico.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "Materiais")

public class MaterialModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column (nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT") // estamos dizendo ao Hibernate que essa coluna deve ser
    // criada como TEXT no banco, em vez de depender do tamanho padrão de uma String/VARCHAR.
    private String descricao;

    @ManyToOne
    @JoinColumn(name = "disciplinas_id", nullable = false )
    private DisciplinasModel disciplina;

    @Column(nullable = false)
    private String nomeArquivo;

    @Column(nullable = false)
    private String tipoArquivo;

    @Column(nullable = false)
    private String caminhoArquivo;

    private Long tamanhoArquivo;
}
