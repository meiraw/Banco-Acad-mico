package br.wm.banco.academico.Model;

import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "respostas_forum")
@Getter @Setter @NoArgsConstructor
public class RespostaForumModel {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discussao_id", nullable = false)
    private DiscussaoModel discussao;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "autor_id")
    private UsuarioModel autor;
    @Column(nullable = false, length = 5000)
    private String conteudo;
    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
    @Column(name = "excluido_em")
    private Instant excluidoEm;
    @Version private long versao;
}
