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
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private br.wm.banco.academico.Enum.TipoPublicacao tipoPublicacao;

    @Column(length = 500)
    private String referenciaBibliografica;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private br.wm.banco.academico.Enum.StatusModeracao statusModeracao = br.wm.banco.academico.Enum.StatusModeracao.PENDENTE;

    private Instant decisaoEm;

    @ManyToOne
    @JoinColumn(name = "moderador_id")
    private UsuarioModel moderador;

    @Column(length = 2000)
    private String justificativaRejeicao;

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
