package br.wm.banco.academico.Service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.poifs.filesystem.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
import java.util.zip.ZipInputStream;

/** O MIME enviado pelo navegador não é usado para decidir o formato. */
@Service
public class ValidacaoArquivoService {
    public static final long LIMITE = 20L * 1024 * 1024;
    private static final Map<String, String> TIPOS = Map.of(
        "pdf", "application/pdf", "doc", "application/msword", "ppt", "application/vnd.ms-powerpoint",
        "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "gif", "image/gif");
    public record Validado(String nomeSeguro, String mimeDetectado, byte[] conteudo) {}

    public Validado validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw invalido("Selecione um arquivo não vazio.");
        if (arquivo.getSize() > LIMITE) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
            "O arquivo deve ter até 20 MB.");
        String nome = nomeSeguro(arquivo.getOriginalFilename());
        String extensao = nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String mime = TIPOS.get(extensao);
        if (mime == null) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Use PDF, DOC, DOCX, PPT, PPTX, JPG, PNG ou GIF.");
        try (var entrada = arquivo.getInputStream()) {
            byte[] bytes = entrada.readNBytes((int) LIMITE + 1);
            if (bytes.length > LIMITE) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
            if (bytes.length == 0) throw invalido("Selecione um arquivo não vazio.");
            switch (extensao) {
                case "pdf" -> validarPdf(bytes);
                case "docx", "pptx" -> validarOfficeXml(bytes, extensao);
                case "doc", "ppt" -> validarOfficeLegado(bytes, extensao);
                default -> validarImagem(bytes, mime);
            }
            return new Validado(nome, mime, bytes);
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof ResponseStatusException status) throw status;
            throw invalido("Arquivo inválido, protegido por senha ou incompatível com a extensão.");
        }
    }

    public static String nomeSeguro(String original) {
        if (original == null || original.isBlank() || original.codePoints().anyMatch(c -> Character.isISOControl(c)
                || Character.getType(c) == Character.FORMAT)) throw invalido("Nome de arquivo inválido.");
        String nome = original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).strip();
        if (nome.length() > 200 || nome.startsWith(".") || nome.endsWith(".") || !nome.contains(".")
                || nome.matches(".*[<>:\"|?*].*")) throw invalido("Nome de arquivo inválido ou muito longo (máximo 200 caracteres).");
        return nome;
    }

    private void validarPdf(byte[] bytes) throws IOException {
        if (bytes.length < 8 || !new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
            throw invalido("O conteúdo não corresponde a um PDF.");
        try (var pdf = Loader.loadPDF(bytes)) {
            if (pdf.isEncrypted() || pdf.getNumberOfPages() < 1 || pdf.getNumberOfPages() > 2000)
                throw invalido("Use um PDF sem senha, com 1 a 2000 páginas.");
            var vistos = Collections.newSetFromMap(new IdentityHashMap<COSBase, Boolean>());
            var fila = new ArrayDeque<COSBase>();
            fila.add(pdf.getDocument().getTrailer());
            Set<String> proibidos = Set.of("JS", "JavaScript", "AA", "OpenAction", "EmbeddedFiles", "EF", "XFA", "RichMedia");
            Set<String> acoes = Set.of("JavaScript", "Launch", "SubmitForm", "ImportData", "GoToR", "GoToE", "Rendition", "Movie", "Sound");
            while (!fila.isEmpty()) {
                COSBase valor = fila.removeFirst();
                if (!vistos.add(valor)) continue;
                if (vistos.size() > 100_000) throw invalido("PDF excessivamente complexo.");
                if (valor instanceof COSObject objeto) {
                    if (objeto.getObject() != null) fila.add(objeto.getObject());
                } else if (valor instanceof COSDictionary dict) {
                    if (acoes.contains(dict.getNameAsString(COSName.S, ""))) throw invalido("PDF com ações ativas não permitido.");
                    if ("URI".equals(dict.getNameAsString(COSName.S, ""))) {
                        try {
                            String esquema = java.net.URI.create(dict.getString(COSName.URI, "")).getScheme();
                            if (esquema == null || !Set.of("https", "http", "mailto").contains(esquema.toLowerCase(Locale.ROOT)))
                                throw invalido("PDF com link ativo inseguro não permitido.");
                        } catch (IllegalArgumentException ex) { throw invalido("PDF com link inválido."); }
                    }
                    for (var chave : dict.keySet()) {
                        if (proibidos.contains(chave.getName())) throw invalido("PDF com scripts, ações ou anexos não permitido.");
                        var filho = dict.getItem(chave);
                        if (filho != null) fila.add(filho);
                    }
                } else if (valor instanceof COSArray array) {
                    for (var filho : array) if (filho != null) fila.add(filho);
                }
                if (fila.size() > 100_000) throw invalido("PDF excessivamente complexo.");
            }
        }
    }

    private void validarOfficeXml(byte[] bytes, String extensao) throws IOException {
        long total = 0;
        Set<String> nomes = new HashSet<>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry item;
            byte[] buffer = new byte[8192];
            while ((item = zip.getNextEntry()) != null) {
                String nome = item.getName().toLowerCase(Locale.ROOT);
                if (!nomes.add(nome) || nomes.size() > 2000 || nome.contains("..") || nome.startsWith("/")
                        || nome.contains("\\") || nome.contains("vbaproject") || nome.contains("/embeddings/")
                        || nome.contains("/activex/")) throw invalido("Pacote Office inseguro ou excessivamente complexo.");
                int lidos;
                while ((lidos = zip.read(buffer)) != -1) {
                    total += lidos;
                    if (total > 80L * 1024 * 1024) throw invalido("O conteúdo descompactado excede 80 MB.");
                }
            }
        }
        String principal = extensao.equals("docx") ? "word/document.xml" : "ppt/presentation.xml";
        if (!nomes.contains("[content_types].xml") || !nomes.contains(principal)) throw invalido("Documento Office inválido.");
        try (var entrada = new ByteArrayInputStream(bytes)) {
            if (extensao.equals("docx")) {
                try (var doc = new XWPFDocument(entrada)) {
                    validarRelacionamentos(doc.getPackage());
                    if (!doc.getPackagePart().getContentType().equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"))
                        throw invalido("Documentos com macros não são permitidos.");
                }
            } else {
                try (var ppt = new XMLSlideShow(entrada)) {
                    validarRelacionamentos(ppt.getPackage());
                    if (!ppt.getPackagePart().getContentType().equals("application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"))
                        throw invalido("Apresentações com macros não são permitidas.");
                }
            }
        }
    }

    private void validarRelacionamentos(org.apache.poi.openxml4j.opc.OPCPackage pacote) throws IOException {
        try {
            for (var parte : pacote.getParts()) {
                String tipo = parte.getContentType().toLowerCase(Locale.ROOT);
                if (tipo.contains("macroenabled") || tipo.contains("vbaproject") || tipo.contains("activex"))
                    throw invalido("Conteúdo ativo Office não permitido.");
                if (parte.isRelationshipPart()) continue;
                for (var relacao : parte.getRelationships()) {
                    String rel = relacao.getRelationshipType();
                    if (rel.endsWith("/oleObject") || rel.endsWith("/package") || rel.endsWith("/control") || rel.endsWith("/attachedTemplate"))
                        throw invalido("Objetos e modelos ativos incorporados não são permitidos.");
                    if (relacao.getTargetMode() == org.apache.poi.openxml4j.opc.TargetMode.EXTERNAL
                            && !relacao.getRelationshipType().endsWith("/hyperlink"))
                        throw invalido("Referências externas ativas não são permitidas.");
                }
            }
        } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException ex) {
            throw new IOException("Relacionamentos inválidos.", ex);
        }
    }

    private void validarOfficeLegado(byte[] bytes, String extensao) throws IOException {
        try (var fs = new POIFSFileSystem(new ByteArrayInputStream(bytes))) {
            var fila = new ArrayDeque<DirectoryEntry>(); fila.add(fs.getRoot());
            int entradas = 0;
            while (!fila.isEmpty()) {
                for (var entry : fila.removeFirst()) {
                    if (++entradas > 2000) throw invalido("Documento Office excessivamente complexo.");
                    String nome = entry.getName().toLowerCase(Locale.ROOT);
                    if (nome.contains("vba") || nome.contains("macro") || nome.equals("objectpool") || nome.equals("encryptedpackage"))
                        throw invalido("Macros, objetos incorporados ou arquivos criptografados não são permitidos.");
                    if (entry instanceof DirectoryEntry dir) fila.add(dir);
                }
            }
            if (extensao.equals("doc")) {
                try (var doc = new HWPFDocument(fs)) { doc.getRange(); }
            } else {
                try (var ppt = new HSLFSlideShow(fs)) { ppt.getSlides(); }
            }
        }
    }

    private void validarImagem(byte[] bytes, String mime) throws IOException {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var leitores = ImageIO.getImageReaders(input);
            if (!leitores.hasNext()) throw invalido("Imagem inválida.");
            var leitor = leitores.next();
            try {
                leitor.setInput(input);
                String formato = leitor.getFormatName().toLowerCase(Locale.ROOT);
                if (!(mime.equals("image/jpeg") ? Set.of("jpeg", "jpg").contains(formato) : mime.equals("image/" + formato)))
                    throw invalido("A imagem não corresponde à extensão.");
                int frames = leitor.getNumImages(true);
                if (frames < 1 || frames > 100) throw invalido("A imagem deve ter até 100 quadros.");
                long pixels = 0;
                for (int i = 0; i < frames; i++) {
                    int largura = leitor.getWidth(i), altura = leitor.getHeight(i);
                    pixels += (long) largura * altura;
                    if (largura <= 0 || altura <= 0 || pixels > 16_000_000) throw invalido("A imagem deve ter até 16 megapixels no total.");
                    if (leitor.read(i) == null) throw invalido("Imagem inválida.");
                }
            } finally { leitor.dispose(); }
        }
    }

    private static ResponseStatusException invalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
