package br.wm.banco.academico.Model;

import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "discussoes_forum")
@Getter @Setter @NoArgsConstructor
public class DiscussaoModel {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 200)
    private String titulo;
    @Column(nullable = false, length = 10000)
    private String conteudo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disciplina_id", nullable = false)
    private DisciplinasModel disciplina;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "autor_id")
    private UsuarioModel autor;
    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
    @Column(name = "excluido_em")
    private Instant excluidoEm;
    @Version private long versao;
}
