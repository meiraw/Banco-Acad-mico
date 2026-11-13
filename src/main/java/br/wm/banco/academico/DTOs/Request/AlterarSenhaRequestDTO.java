package br.wm.banco.academico.DTOs.Request;
import jakarta.validation.constraints.*;

public record AlterarSenhaRequestDTO(
    @NotBlank(message = "Informe a senha atual.") @Size(max = 72, message = "Senha atual inválida.") String senhaAtual,
    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 8, max = 72, message = "A nova senha deve ter pelo menos 8 caracteres e até 72 bytes UTF-8.") String novaSenha,
    @NotBlank(message = "Confirme a nova senha.") @Size(max = 72, message = "Confirmação de senha inválida.") String confirmacaoNovaSenha
) {}
