package br.wm.banco.academico;

import br.wm.banco.academico.DTOs.Request.MaterialRequestDTO;
import br.wm.banco.academico.Enum.TipoPublicacao;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

final class UploadFixtures {
    private static final byte[] PDF = criarPdf();
    static byte[] pdf() { return PDF.clone(); }
    private static byte[] criarPdf() {
        try (var doc = new PDDocument(); var out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage()); doc.save(out); return out.toByteArray();
        } catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
    }
    static MaterialRequestDTO dados(String titulo, String descricao, UUID disciplina, TipoPublicacao tipo, String referencia) {
        var dto = new MaterialRequestDTO(titulo, descricao, disciplina, tipo, referencia);
        dto.setResponsabilidadeConfirmada(true); return dto;
    }
}
