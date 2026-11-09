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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.time.Instant;
import br.wm.banco.academico.Enum.StatusModeracao;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class MaterialService {


    //Crie uma variável chamada uploadPath que representa o caminho da pasta uploads.
    private final Path uploadPath  = Paths.get("Uploads"); // Onde fica os arquivos
    //Path é o que representa um caminho
    //uploadPath nome da nossa variavel
    //uploads  nome da pasta que requeremos representar

    private final MaterialRepository materialRepository;
    private final DisciplinasService disciplinasService;// dependências
    private final UsuarioRepository usuarios;
    private final ApplicationEventPublisher eventos;
    private final LimpezaArquivoService limpeza;
    public MaterialService(
            MaterialRepository materialRepository,
            DisciplinasService disciplinasService,
            UsuarioRepository usuarios, ApplicationEventPublisher eventos, LimpezaArquivoService limpeza){
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

        MaterialModel material = new MaterialModel();
        material.setAutor(usuarios.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
        material.setPublicadoEm(Instant.now());
        material.setStatusModeracao(StatusModeracao.PENDENTE);
        DisciplinasModel disciplina = disciplinasService.buscarPorId(dto.getDisciplinaId()); // Criamos o relacionamento
        // de disciplinas com o materia , então , fazemos a injeção de dependencia de disciplinas
        //depois colocamos a disciplinas no cria() de material

        String nomeArquivo = arquivo.getOriginalFilename();
        String nomeUnico = UUID.randomUUID().toString();
        Path caminhoArquivo = uploadPath.resolve(nomeUnico);// Aqui definimos o caminho para os arquivos // Onde ficará este arquivo especificos
        System.out.println("ARQUIVO SALVO EM: " + caminhoArquivo.toAbsolutePath());

        try { // Tentar
            Files.createDirectories(uploadPath);
            Files.copy(
                    arquivo.getInputStream(),//de onde //copiar um arquivo/conteúdo de um lugar para outro.
                    caminhoArquivo // para onde
            );
        }catch(IOException e){ // Se der erro exiba essa imagem
            throw new RuntimeException("Erro ao salvar o arquivo",e ); // Ocorreu esse problema; pare a execução e lance esse erro.
        }
        material.setTitulo(dto.getTitulo());
        material.setDescricao(dto.getDescricao());
        aplicarPublicacao(material, dto);

        // material.setNomeArquivo(dto.getNomeArquivo()); antes da implementação do multipartFile
        material.setNomeArquivo(nomeArquivo); //Depois  , aqui temos o getOriginalFilename , para os nomes do arquivos
        //Pega informações do arquivo
        material.setTipoArquivo(arquivo.getContentType()); // Para os tipos de arquivos
        material.setCaminhoArquivo(caminhoArquivo.toString());
        material.setTamanhoArquivo(arquivo.getSize());//O tamanho do arquivo

        material.setDisciplina(disciplina); // esse é o set de disciplinas


        // Se a transação falhar, o arquivo recém-criado não fica órfão.
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            try { Files.deleteIfExists(caminhoArquivo); }
                            catch (IOException ex) { org.slf4j.LoggerFactory.getLogger(MaterialService.class)
                                    .warn("Falha ao limpar arquivo de upload revertido."); }
                        }
                    }
                });
        }
        MaterialModel salvo = materialRepository.save(material);
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
        material.setStatusModeracao(aprovar ? StatusModeracao.APROVADO : StatusModeracao.REJEITADO);
        material.setDecisaoEm(Instant.now());
        material.setModerador(usuarios.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
        material.setJustificativaRejeicao(aprovar ? null : justificativa.strip());
        return new MaterialResponseDTO(materialRepository.save(material));
    }

    public Resource arquivo(UUID id) {
        var material = buscarPorId(id);
        Path raiz = uploadPath.toAbsolutePath().normalize();
        Path caminho = Paths.get(material.getCaminhoArquivo()).toAbsolutePath().normalize();
        if (!caminho.startsWith(raiz) || !Files.isRegularFile(caminho)) {
            throw new ResourceNotFoundException("Arquivo não encontrado.");
        }
        return new FileSystemResource(caminho);
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
