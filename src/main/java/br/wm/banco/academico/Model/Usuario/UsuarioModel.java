package br.wm.banco.academico.Model.Usuario;


import br.wm.banco.academico.Enum.PerfilUsuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
@Table(name = "usuarios")
public class UsuarioModel {
    public static final String CURSO_ACADEMICO = "Ciência da Computação";

    @Column(nullable = false, length = 200)
    private String curso = CURSO_ACADEMICO;

    @Setter
    @Column(name = "semestre_atual")
    private Integer semestreAtual;

    @Setter
    @Column(name = "email_confirmado", nullable = false)
    private boolean emailConfirmado;

    @Setter
    @Column(name = "versao_credenciais", nullable = false)
    private long versaoCredenciais;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Column(nullable = false )
    private String nome;

    @Setter
    @Column(nullable = false , unique = true )
    private String email;

    @Setter
    @Column(nullable = false )
    private String senha;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false )
    private PerfilUsuario perfil;
}
