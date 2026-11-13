package br.wm.banco.academico.DTOs.Response;
import java.time.Instant;
import java.util.UUID;
public record DiscussaoResponseDTO(UUID id, String titulo, String conteudo, UUID disciplinaId,
    String disciplinaNome, UUID autorId, String autorNome, Instant criadoEm, Instant atualizadoEm,
    long totalRespostas, long versao) {}
