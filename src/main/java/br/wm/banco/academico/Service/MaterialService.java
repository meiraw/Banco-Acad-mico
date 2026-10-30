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
    public MaterialService(
            MaterialRepository materialRepository,
            DisciplinasService disciplinasService,
            UsuarioRepository usuarios){
        this.usuarios = usuarios;
        this.materialRepository = materialRepository; // Injeção de dependencia de materialrepository
        this.disciplinasService = disciplinasService;// Injeçaõ de dependencia de disciplinas service
    }

    //Aplicando  o MultipartFile no cria do Material
    //Graça ao MultipartFile , recebemos o tipo de arquivos ; PDF ,DOCX ,PPTX...
    public MaterialModel criar (MaterialRequestDTO dto , MultipartFile arquivo, String email) {

        MaterialModel material = new MaterialModel();
        material.setAutor(usuarios.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
        material.setPublicadoEm(Instant.now());
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

        // material.setNomeArquivo(dto.getNomeArquivo()); antes da implementação do multipartFile
        material.setNomeArquivo(nomeArquivo); //Depois  , aqui temos o getOriginalFilename , para os nomes do arquivos
        //Pega informações do arquivo
        material.setTipoArquivo(arquivo.getContentType()); // Para os tipos de arquivos
        material.setCaminhoArquivo(caminhoArquivo.toString());
        material.setTamanhoArquivo(arquivo.getSize());//O tamanho do arquivo

        material.setDisciplina(disciplina); // esse é o set de disciplinas


        return materialRepository.save(material);
    }

    public Page<MaterialModel> listarTudo (Pageable pageable){
        return materialRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<MaterialResponseDTO> minhasPublicacoes(String email, int pagina) {
        if (pagina < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Página inválida.");
        return materialRepository.findByAutorEmailOrderByPublicadoEmDescIdDesc(
                email, PageRequest.of(pagina, 12)).map(MaterialResponseDTO::new);
    }

    public MaterialModel buscarPorId (UUID id){
        return materialRepository.findById(id).orElseThrow( () -> new ResourceNotFoundException("O id "+id+"não foi encontrado!"));
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


    public MaterialModel atualizar (MaterialRequestDTO dto, UUID id){
        MaterialModel novoMaterial = buscarPorId(id);
        DisciplinasModel novaDisciplina = disciplinasService.buscarPorId(dto.getDisciplinaId()); // Criamos o relacionamento
        // de disciplinas com o materia , então , fazemos a injeção de dependencia de disciplinas
        //depois colocamos a disciplinas no cria() de material

        novoMaterial.setTitulo(dto.getTitulo());
        novoMaterial.setDescricao(dto.getDescricao());
        novoMaterial.setDisciplina(novaDisciplina); // esse é o set de disciplinas

        return materialRepository.save(novoMaterial);
    }

    public void excluir(UUID id){

        MaterialModel remover = buscarPorId(id);

        Path caminhoArquivo = Paths.get(remover.getCaminhoArquivo());

        try {
            Files.deleteIfExists(caminhoArquivo);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao excluir o arquivo", e);
        }

        materialRepository.delete(remover);
    }
}
