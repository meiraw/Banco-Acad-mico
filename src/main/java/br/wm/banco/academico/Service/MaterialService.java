package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Request.MaterialRequestDTO;
import br.wm.banco.academico.DTOs.Response.MaterialResponseDTO;
import br.wm.banco.academico.Exception.ResourceNotFoundException;
import br.wm.banco.academico.Model.DisciplinasModel;
import br.wm.banco.academico.Model.MaterialModel;
import br.wm.banco.academico.Repository.MaterialRepository;
import br.wm.banco.academico.Repository.UsuarioRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.time.Instant;
import br.wm.banco.academico.Enum.StatusModeracao;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class MaterialService {


    private final ArmazenamentoService armazenamento;
    private final ValidacaoArquivoService validacao;
    private final AntimalwareService antimalware;
    private final LimiteUploadService limites;
    private final long cota;

    private final MaterialRepository materialRepository;
    private final DisciplinasService disciplinasService;// dependências
    private final UsuarioRepository usuarios;
    private final ApplicationEventPublisher eventos;
    private final LimpezaArquivoService limpeza;
    public MaterialService(
            MaterialRepository materialRepository,
            DisciplinasService disciplinasService,
            UsuarioRepository usuarios, ApplicationEventPublisher eventos, LimpezaArquivoService limpeza,
            ArmazenamentoService armazenamento, ValidacaoArquivoService validacao,
            AntimalwareService antimalware, LimiteUploadService limites,
            @org.springframework.beans.factory.annotation.Value("${app.upload.cota-usuario:524288000}") long cota){
        this.armazenamento = armazenamento; this.validacao = validacao;
        this.antimalware = antimalware; this.limites = limites; this.cota = cota;
        if (cota < 1) throw new IllegalArgumentException("Cota de upload inválida.");
        this.limpeza = limpeza;
        this.eventos = eventos;
        this.usuarios = usuarios;
        this.materialRepository = materialRepository; // Injeção de dependencia de materialrepository
        this.disciplinasService = disciplinasService;// Injeçaõ de dependencia de disciplinas service
    }

    //Aplicando  o MultipartFile no cria do Material
    //Graça ao MultipartFile , recebemos o tipo de arquivos ; PDF ,DOCX ,PPTX...
    @Transactional
    public MaterialModel criar (MaterialRequestDTO dto , MultipartFile arquivo, String email) {
        validarPublicacao(dto);
        try (var permissao = limites.adquirir(email, arquivo.getSize())) {
            return criarValidado(dto, arquivo, email);
        }
    }

    private MaterialModel criarValidado(MaterialRequestDTO dto, MultipartFile arquivo, String email) {
        var validado = validacao.validar(arquivo);
        antimalware.verificar(validado.conteudo());

        MaterialModel material = new MaterialModel();
        var conta = usuarios.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        // Serializa a verificação da cota entre instâncias e uploads do mesmo autor.
        material.setAutor(usuarios.bloquearPorId(conta.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
        long utilizado = materialRepository.bytesDoAutor(conta.getId());
        if (validado.conteudo().length > cota - utilizado)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Sua cota de armazenamento de materiais foi atingida.");
        material.setPublicadoEm(Instant.now());
        material.setStatusModeracao(StatusModeracao.PENDENTE);
        DisciplinasModel disciplina = disciplinasService.buscarPorId(dto.getDisciplinaId()); // Criamos o relacionamento
        // de disciplinas com o materia , então , fazemos a injeção de dependencia de disciplinas
        //depois colocamos a disciplinas no cria() de material

        String nomeArquivo = validado.nomeSeguro();
        Path caminhoArquivo = armazenamento.novoCaminho();
        material.setTitulo(dto.getTitulo());
        material.setDescricao(dto.getDescricao());
        aplicarPublicacao(material, dto);

        // material.setNomeArquivo(dto.getNomeArquivo()); antes da implementação do multipartFile
        material.setNomeArquivo(nomeArquivo); //Depois  , aqui temos o getOriginalFilename , para os nomes do arquivos
        //Pega informações do arquivo
        material.setTipoArquivo(validado.mimeDetectado());
        material.setCaminhoArquivo(armazenamento.referencia(caminhoArquivo));
        material.setTamanhoArquivo((long) validado.conteudo().length);

        material.setDisciplina(disciplina); // esse é o set de disciplinas


        // Se a transação falhar, o arquivo recém-criado não fica órfão.
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            armazenamento.remover(caminhoArquivo);
                        }
                    }
                });
        }
        armazenamento.salvar(caminhoArquivo, validado.conteudo());
        MaterialModel salvo;
        try { salvo = materialRepository.save(material); }
        catch (RuntimeException ex) { armazenamento.remover(caminhoArquivo); throw ex; }
        eventos.publishEvent(new ModeracaoEmailService.MaterialPendente(salvo.getId(), salvo.getTitulo(),
                disciplina.getNome(), salvo.getAutor().getNome(), salvo.getPublicadoEm()));
        return salvo;
    }

    public Page<MaterialModel> listarTudo (Pageable pageable){
        return materialRepository.findByStatusModeracao(StatusModeracao.APROVADO, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MaterialResponseDTO> minhasPublicacoes(String email, int pagina) {
        if (pagina < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Página inválida.");
        return materialRepository.findByAutorEmailOrderByPublicadoEmDescIdDesc(
                email, PageRequest.of(pagina, 12)).map(MaterialResponseDTO::new);
    }

    public MaterialModel buscarPorId (UUID id){
        MaterialModel material = materialRepository.findById(id).orElseThrow( () -> new ResourceNotFoundException("Material não encontrado."));
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean autor = auth != null && auth.isAuthenticated() && material.getAutor() != null
                && material.getAutor().getEmail().equals(auth.getName());
        if (material.getStatusModeracao() != StatusModeracao.APROVADO && !administrador() && !autor)
            throw new ResourceNotFoundException("Material não encontrado.");
        return material;
    }

    private boolean administrador() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void exigirAdministrador() {
        if (!administrador()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito a administradores.");
    }

    @Transactional(readOnly = true)
    public Page<MaterialResponseDTO> pendentes(int pagina) {
        exigirAdministrador();
        if (pagina < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Página inválida.");
        return materialRepository.findByStatusModeracao(StatusModeracao.PENDENTE,
                PageRequest.of(pagina, 12, org.springframework.data.domain.Sort.by("publicadoEm", "id").ascending()))
                .map(MaterialResponseDTO::new);
    }

    @Transactional
    public MaterialResponseDTO decidir(UUID id, boolean aprovar, String justificativa) {
        exigirAdministrador();
        if (!aprovar && (justificativa == null || justificativa.isBlank() || justificativa.length() > 2000))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma justificativa de até 2000 caracteres.");
        // O bloqueio serializa decisões concorrentes, inclusive de administradores diferentes.
        MaterialModel material = materialRepository.bloquearPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado."));
        if (material.getStatusModeracao() != StatusModeracao.PENDENTE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este material já foi analisado. Atualize a fila.");
        if (aprovar) antimalware.verificar(armazenamento.permitido(material.getCaminhoArquivo()));
        material.setStatusModeracao(aprovar ? StatusModeracao.APROVADO : StatusModeracao.REJEITADO);
        material.setDecisaoEm(Instant.now());
        material.setModerador(usuarios.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
        material.setJustificativaRejeicao(aprovar ? null : justificativa.strip());
        return new MaterialResponseDTO(materialRepository.save(material));
    }

    public Resource arquivo(UUID id) {
        var material = buscarPorId(id);
        Path caminho = armazenamento.permitido(material.getCaminhoArquivo());
        if (!Files.isRegularFile(caminho, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new ResourceNotFoundException("Arquivo não encontrado.");
        }
        return new FileSystemResource(caminho) {
            @Override public java.io.InputStream getInputStream() throws java.io.IOException {
                return Files.newInputStream(caminho, java.nio.file.LinkOption.NOFOLLOW_LINKS);
            }
            @Override public java.nio.channels.ReadableByteChannel readableChannel() throws java.io.IOException {
                return Files.newByteChannel(caminho, java.nio.file.StandardOpenOption.READ, java.nio.file.LinkOption.NOFOLLOW_LINKS);
            }
        };
    }


    @Transactional
    public MaterialModel atualizar (MaterialRequestDTO dto, UUID id){
        exigirAdministrador();
        validarPublicacao(dto);
        MaterialModel novoMaterial = materialRepository.bloquearPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado."));
        DisciplinasModel novaDisciplina = disciplinasService.buscarPorId(dto.getDisciplinaId()); // Criamos o relacionamento
        // de disciplinas com o materia , então , fazemos a injeção de dependencia de disciplinas
        //depois colocamos a disciplinas no cria() de material

        novoMaterial.setTitulo(dto.getTitulo());
        novoMaterial.setDescricao(dto.getDescricao());
        aplicarPublicacao(novoMaterial, dto);
        novoMaterial.setDisciplina(novaDisciplina); // esse é o set de disciplinas

        return materialRepository.save(novoMaterial);
    }

    private void validarPublicacao(MaterialRequestDTO dto) {
        if (dto.getTipoPublicacao() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione o tipo de publicação.");
        if (!dto.isReferenciaObrigatoriaPreenchida())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a referência bibliográfica do artigo.");
        if (!dto.isReferenciaCompativel())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A referência bibliográfica só é permitida para Livro ou Artigo.");
        if (dto.getReferenciaBibliografica() != null && dto.getReferenciaBibliografica().length() > 500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A referência bibliográfica deve ter até 500 caracteres.");
    }

    private void aplicarPublicacao(MaterialModel material, MaterialRequestDTO dto) {
        material.setTipoPublicacao(dto.getTipoPublicacao());
        String referencia = dto.getReferenciaBibliografica();
        material.setReferenciaBibliografica(referencia == null || referencia.isBlank() ? null : referencia.strip());
    }

    @Transactional
    public void excluir(UUID id){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        MaterialModel remover = materialRepository.bloquearPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado."));
        boolean autor = remover.getAutor() != null && remover.getAutor().getEmail().equals(auth.getName());
        if (!administrador() && !autor)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você só pode excluir suas próprias publicações.");
        limpeza.agendar(id, remover.getCaminhoArquivo());
        materialRepository.delete(remover);
        materialRepository.flush();
    }
}
