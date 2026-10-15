package br.wm.banco.academico.DTOs.Request;
import jakarta.validation.constraints.*;
public record RedefinirSenhaRequestDTO(
 @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Token inválido ou expirado.") String token,
 @NotBlank @Size(min = 8, max = 72) String novaSenha) {}
