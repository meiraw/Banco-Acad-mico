package br.wm.banco.academico.DTOs.Response;
import java.time.Instant;
import java.util.UUID;
public record DiscussaoResumoDTO(UUID id, String titulo, String previa, UUID disciplinaId,
    String disciplinaNome, UUID autorId, String autorNome, Instant criadoEm, long totalRespostas) {}
