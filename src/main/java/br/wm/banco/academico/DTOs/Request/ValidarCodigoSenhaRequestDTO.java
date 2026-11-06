package br.wm.banco.academico.DTOs.Request;
import jakarta.validation.constraints.*;
public record ValidarCodigoSenhaRequestDTO(
 @NotBlank @Email @Size(max = 254) String email,
 @NotBlank @Pattern(regexp = "[0-9]{4}", message = "Informe o codigo de 4 digitos.") String codigo) {}
