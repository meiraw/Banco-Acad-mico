package br.wm.banco.academico.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;

import java.util.UUID;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "materiais")

public class MaterialModel {
    @ManyToOne
    @JoinColumn(name = "autor_id")
    private UsuarioModel autor;

    private Instant publicadoEm;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column (nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT") // estamos dizendo ao Hibernate que essa coluna deve ser
    // criada como TEXT no banco, em vez de depender do tamanho padrão de uma String/VARCHAR.
    private String descricao;

    @ManyToOne
    @JoinColumn(name = "disciplina_id", nullable = false )
    private DisciplinasModel disciplina;

    @Column(nullable = false)
    private String nomeArquivo;

    @Column(nullable = false)
    private String tipoArquivo;

    @Column(nullable = false)
    private String caminhoArquivo;

    private Long tamanhoArquivo;
}
