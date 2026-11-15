package br.wm.banco.academico;

import br.wm.banco.academico.Service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class UploadSegurancaTests {
    @TempDir Path pasta;
    final ValidacaoArquivoService validacao = new ValidacaoArquivoService();
    MockMultipartFile arquivo(String nome, String mime, byte[] bytes) {
        return new MockMultipartFile("arquivo", nome, mime, bytes);
    }
    @Test void rejeitaVazioExcessoConteudoDisfarcadoENomeInvalido() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
            () -> validacao.validar(arquivo("a.pdf", "application/pdf", new byte[0]))).getStatusCode().value());
        assertEquals(413, assertThrows(ResponseStatusException.class,
            () -> validacao.validar(arquivo("a.pdf", "application/pdf", new byte[(int) ValidacaoArquivoService.LIMITE + 1]))).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> validacao.validar(arquivo("a.pdf", "application/pdf", "<html>conteúdo</html>".getBytes())));
        assertThrows(ResponseStatusException.class, () -> validacao.validar(arquivo("a.pdf", "application/pdf", "%PDF-1.7\n%%EOF".getBytes())));
        assertThrows(ResponseStatusException.class, () -> validacao.validar(arquivo("a.svg", "image/png", UploadFixtures.pdf())));
        for (String nome : new String[]{"a\r\nb.pdf", "a".repeat(201) + ".pdf", ".pdf", "a:b.pdf"})
            assertThrows(ResponseStatusException.class, () -> ValidacaoArquivoService.nomeSeguro(nome));
    }
    @Test void detectaPdfRealSemConfiarNoMimeELimpaCaminhoDoNome() {
        var pdf = validacao.validar(arquivo("C:\\fakepath\\resumo.PDF", "text/html", UploadFixtures.pdf()));
        assertEquals("resumo.PDF", pdf.nomeSeguro());
        assertEquals("application/pdf", pdf.mimeDetectado());
        assertArrayEquals(UploadFixtures.pdf(), pdf.conteudo());
        assertEquals("a.pdf", ValidacaoArquivoService.nomeSeguro("../../a.pdf"));
    }
    @Test void rejeitaPdfComJavaScript() throws Exception {
        try (var doc = new org.apache.pdfbox.pdmodel.PDDocument(); var out = new ByteArrayOutputStream()) {
            doc.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            doc.getDocumentCatalog().setOpenAction(new org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript("app.alert('x')"));
            doc.save(out);
            assertThrows(ResponseStatusException.class, () -> validacao.validar(arquivo("a.pdf", "application/pdf", out.toByteArray())));
        }
    }
    @Test void verificaImagemRealEExtensao() throws Exception {
        var out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20, 20, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
        assertEquals("image/png", validacao.validar(arquivo("a.png", "application/octet-stream", out.toByteArray())).mimeDetectado());
        assertThrows(ResponseStatusException.class, () -> validacao.validar(arquivo("a.jpg", "image/jpeg", out.toByteArray())));
    }
    @Test void aceitaOfficeEstruturadoERejeitaExtensaoTrocada() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            doc.createParagraph().createRun().setText("Material acadêmico"); doc.write(out);
        }
        byte[] docx = out.toByteArray();
        assertTrue(validacao.validar(arquivo("a.docx", null, docx)).mimeDetectado().contains("wordprocessingml"));
        assertThrows(ResponseStatusException.class, () -> validacao.validar(arquivo("a.pptx", "application/zip", docx)));
        out.reset();
        try (var ppt = new org.apache.poi.xslf.usermodel.XMLSlideShow()) { ppt.createSlide(); ppt.write(out); }
        assertTrue(validacao.validar(arquivo("a.pptx", null, out.toByteArray())).mimeDetectado().contains("presentationml"));
        out.reset();
        try (var ppt = new org.apache.poi.hslf.usermodel.HSLFSlideShow()) { ppt.createSlide(); ppt.write(out); }
        assertEquals("application/vnd.ms-powerpoint", validacao.validar(arquivo("a.ppt", null, out.toByteArray())).mimeDetectado());
    }
    @Test void rejeitaMacroEZipComExpansaoExcessiva() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("word/vbaProject.bin")); zip.write(new byte[10]); zip.closeEntry();
        }
        assertThrows(ResponseStatusException.class, () -> validacao.validar(arquivo("a.docx", null, out.toByteArray())));
        out.reset();
        try (var zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            byte[] bloco = new byte[1024 * 1024];
            for (int i = 0; i < 81; i++) zip.write(bloco);
            zip.closeEntry();
        }
        assertThrows(ResponseStatusException.class, () -> validacao.validar(arquivo("a.docx", null, out.toByteArray())));
    }
    @Test void gravaCompletoRejeitaCaminhoExternoELimpaFalha() throws Exception {
        var storage = new ArmazenamentoService(pasta.toString(), 0);
        Path destino = storage.novoCaminho(); storage.salvar(destino, UploadFixtures.pdf());
        assertArrayEquals(UploadFixtures.pdf(), Files.readAllBytes(destino));
        assertTrue(ArmazenamentoService.validado(destino.toString()));
        assertFalse(ArmazenamentoService.validado(pasta.resolve("legado").toString()));
        assertThrows(br.wm.banco.academico.Exception.ResourceNotFoundException.class,
            () -> storage.permitido(pasta.getParent().resolve("fora").toString()));
        Path indisponivel = pasta.resolve("subpasta/arquivo");
        assertThrows(br.wm.banco.academico.Exception.ResourceNotFoundException.class,
            () -> storage.salvar(indisponivel, new byte[1]));
        var cheio = new ArmazenamentoService(pasta.toString(), Long.MAX_VALUE);
        assertEquals(507, assertThrows(ResponseStatusException.class,
            () -> cheio.salvar(cheio.novoCaminho(), new byte[1])).getStatusCode().value());
        try (var arquivos = Files.list(pasta)) { assertEquals(1, arquivos.count()); }
    }
    @Test void limitaConcorrenciaFrequenciaEVolumeSemVazarPermissoes() {
        var limites = new LimiteUploadService(1, 2, 10);
        try (var a = limites.adquirir("a", 3)) {
            assertThrows(ResponseStatusException.class, () -> limites.adquirir("a", 1));
            assertThrows(ResponseStatusException.class, () -> limites.adquirir("b", 1));
        }
        try (var b = limites.adquirir("b", 1)) { assertNotNull(b); }
        try (var a = limites.adquirir("a", 7)) { assertNotNull(a); }
        assertThrows(ResponseStatusException.class, () -> limites.adquirir("a", 0));
        var volume = new LimiteUploadService(1, 10, 5);
        assertThrows(ResponseStatusException.class, () -> volume.adquirir("c", 6));
        try (var c = volume.adquirir("c", 5)) { assertNotNull(c); }
        assertThrows(ResponseStatusException.class, () -> volume.adquirir("c", 1));
    }
    @Test void limpaTemporarioQuandoPublicacaoDoArquivoFalha() throws Exception {
        var storage = new ArmazenamentoService(pasta.toString(), 0);
        Path destino = storage.novoCaminho();
        Files.createDirectory(destino); Files.writeString(destino.resolve("ocupado"), "preservar");
        assertThrows(ResponseStatusException.class, () -> storage.salvar(destino, UploadFixtures.pdf()));
        try (var itens = Files.list(pasta)) { assertEquals(1, itens.count()); }
        assertEquals("preservar", Files.readString(destino.resolve("ocupado")));
    }
    @Test void rejeitaLinksSimbolicosNaLeituraEExclusao() throws Exception {
        Path arquivo = pasta.resolve("original"); Files.writeString(arquivo, "conteúdo");
        Path link = pasta.resolve("link");
        try { Files.createSymbolicLink(link, arquivo); }
        catch (IOException | UnsupportedOperationException ex) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Ambiente sem permissão para criar links simbólicos.");
        }
        var storage = new ArmazenamentoService(pasta.toString(), 0);
        assertThrows(br.wm.banco.academico.Exception.ResourceNotFoundException.class, () -> storage.permitido(link.toString()));
        storage.remover(link);
        assertTrue(Files.exists(arquivo)); assertTrue(Files.isSymbolicLink(link));
    }
    @Test void referenciaRelativaSobreviveTrocaDeRaizSemAlterarArquivoAntigo() throws Exception {
        var antiga = new ArmazenamentoService(pasta.resolve("antiga").toString(), 0);
        var nova = new ArmazenamentoService(pasta.resolve("nova").toString(), 0);
        Path original = antiga.novoCaminho(); antiga.salvar(original, UploadFixtures.pdf());
        String referencia = antiga.referencia(original);
        assertEquals(original.getFileName().toString(), referencia);
        Files.createDirectories(pasta.resolve("nova"));
        Files.copy(original, pasta.resolve("nova").resolve(referencia));
        assertArrayEquals(UploadFixtures.pdf(), Files.readAllBytes(nova.permitido(referencia)));
        assertTrue(Files.exists(original));
        assertEquals(original, antiga.permitido(original.toString()));
    }
    @Test void importaSomenteRaizesLegadasExplicitasDoWindowsELinux() throws Exception {
        String nome = "validado-v1-" + java.util.UUID.randomUUID();
        var storage = new ArmazenamentoService(pasta.toString(), 0, "C:\\antigo\\Uploads;/srv/antigo/Uploads");
        storage.salvar(pasta.resolve(nome), UploadFixtures.pdf());
        assertEquals(pasta.resolve(nome), storage.permitido("c:\\antigo\\Uploads\\" + nome));
        assertEquals(pasta.resolve(nome), storage.permitido("/srv/antigo/Uploads/" + nome));
        assertTrue(ArmazenamentoService.validado("C:\\antigo\\Uploads\\" + nome));
        for (String caminho : new String[]{"C:\\outro\\" + nome, "/srv/antigo/Uploads-extra/" + nome,
                "C:\\antigo\\Uploads\\..\\" + nome, "/srv/antigo/Uploads/sub/" + nome,
                "../" + nome, "sub/" + nome, "", "C:arquivo"}) {
            assertThrows(br.wm.banco.academico.Exception.ResourceNotFoundException.class, () -> storage.permitido(caminho));
        }
        var semMapeamento = new ArmazenamentoService(pasta.toString(), 0);
        assertThrows(br.wm.banco.academico.Exception.ResourceNotFoundException.class,
            () -> semMapeamento.permitido("C:\\antigo\\Uploads\\" + nome));
        assertThrows(IllegalArgumentException.class, () -> new ArmazenamentoService(pasta.toString(), 0, "../Uploads"));
    }
    @Test void exigeCotaAntesDeGravarSemCheckboxDeResponsabilidade() {
        var repo = org.mockito.Mockito.mock(br.wm.banco.academico.Repository.MaterialRepository.class);
        var usuarios = org.mockito.Mockito.mock(br.wm.banco.academico.Repository.UsuarioRepository.class);
        var conta = new br.wm.banco.academico.Model.Usuario.UsuarioModel();
        org.springframework.test.util.ReflectionTestUtils.setField(conta, "id", java.util.UUID.randomUUID());
        org.mockito.Mockito.when(usuarios.findByEmail("autor")).thenReturn(java.util.Optional.of(conta));
        org.mockito.Mockito.when(usuarios.bloquearPorId(conta.getId())).thenReturn(java.util.Optional.of(conta));
        var storage = new ArmazenamentoService(pasta.toString(), 0);
        var service = new MaterialService(repo, org.mockito.Mockito.mock(DisciplinasService.class), usuarios,
            org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class), org.mockito.Mockito.mock(LimpezaArquivoService.class),
            storage, validacao, org.mockito.Mockito.mock(AntimalwareService.class), new LimiteUploadService(2, 20, 104857600), 1);
        var dto = UploadFixtures.dados("Título", "Descrição", java.util.UUID.randomUUID(), br.wm.banco.academico.Enum.TipoPublicacao.RESUMO, null);
        dto.setResponsabilidadeConfirmada(null);
        assertEquals(413, assertThrows(ResponseStatusException.class,
            () -> service.criar(dto, arquivo("a.pdf", "application/pdf", UploadFixtures.pdf()), "autor")).getStatusCode().value());
        org.mockito.Mockito.verify(repo, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }
}
