package br.wm.banco.academico.DTOs.Request;
import jakarta.validation.constraints.*;
public record EsqueciSenhaRequestDTO(
 @NotBlank @Email @Size(max = 200) String email) {}
