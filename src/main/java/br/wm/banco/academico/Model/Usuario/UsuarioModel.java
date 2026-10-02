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
