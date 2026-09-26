package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Request.MaterialRequestDTO;
import br.wm.banco.academico.Exception.ResourceNotFoundException;
import br.wm.banco.academico.Model.DisciplinasModel;
import br.wm.banco.academico.Model.MaterialModel;
import br.wm.banco.academico.Repository.MaterialRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class MaterialService {


    //Crie uma variável chamada uploadPath que representa o caminho da pasta uploads.
    private final Path uploadPath  = Paths.get("Uploads"); // Onde fica os arquivos
    //Path é o que representa um caminho
    //uploadPath nome da nossa variavel
    //uploads  nome da pasta que requeremos representar

    private final MaterialRepository materialRepository;
    private final DisciplinasService disciplinasService;// dependências
    public MaterialService(
            MaterialRepository materialRepository,
            DisciplinasService disciplinasService){
        this.materialRepository = materialRepository; // Injeção de dependencia de materialrepository
        this.disciplinasService = disciplinasService;// Injeçaõ de dependencia de disciplinas service
    }

    //Aplicando  o MultipartFile no cria do Material
    //Graça ao MultipartFile , recebemos o tipo de arquivos ; PDF ,DOCX ,PPTX...
    public MaterialModel criar (MaterialRequestDTO dto , MultipartFile arquivo) {

        MaterialModel material = new MaterialModel();
        DisciplinasModel disciplina = disciplinasService.buscarPorId(dto.getDisciplinaId()); // Criamos o relacionamento
        // de disciplinas com o materia , então , fazemos a injeção de dependencia de disciplinas
        //depois colocamos a disciplinas no cria() de material

        String nomeArquivo = arquivo.getOriginalFilename(); // Isso defini o nome do arquivo
        Path caminhoArquivo = uploadPath.resolve(nomeArquivo); // Aqui definimos o caminho para os arquivos // Onde ficará este arquivo especificos

        try { // Tentar
            Files.copy(
                    arquivo.getInputStream(),//de onde //copiar um arquivo/conteúdo de um lugar para outro.
                    caminhoArquivo // para onde
            );
        }catch(IOException e){ // Se der erro exiba essa imagem
            throw new RuntimeException(e.getMessage()); // Ocorreu esse problema; pare a execução e lance esse erro.
        }
        material.setTitulo(dto.getTitulo());
        material.setDescricao(dto.getDescricao());

        // material.setNomeArquivo(dto.getNomeArquivo()); antes da implementação do multipartFile
        material.setNomeArquivo(arquivo.getOriginalFilename()); //Depois  , aqui temos o getOriginalFilename , para os nomes do arquivos
        //Pega informações do arquivo
        material.setTipoArquivo(arquivo.getContentType()); // Para os tipos de arquivos
        material.setCaminhoArquivo(dto.getCaminhoArquivo());
        material.setTamanhoArquivo(arquivo.getSize());//O tamanho do arquivo

        material.setDisciplina(disciplina); // esse é o set de disciplinas


        return materialRepository.save(material);
    }

    public Page<MaterialModel> listarTudo (Pageable pageable){
        return materialRepository.findAll(pageable);
    }

    public MaterialModel buscarPorId (UUID id){
        return materialRepository.findById(id).orElseThrow( () -> new ResourceNotFoundException("O id "+id+"não foi encontrado!"));
    }


    public MaterialModel atualizar (MaterialRequestDTO dto, UUID id){
        MaterialModel novoMaterial = buscarPorId(id);
        DisciplinasModel novaDisciplina = disciplinasService.buscarPorId(dto.getDisciplinaId()); // Criamos o relacionamento
        // de disciplinas com o materia , então , fazemos a injeção de dependencia de disciplinas
        //depois colocamos a disciplinas no cria() de material

        novoMaterial.setTitulo(dto.getTitulo());
        novoMaterial.setDescricao(dto.getDescricao());
        novoMaterial.setNomeArquivo(dto.getNomeArquivo());
        novoMaterial.setTipoArquivo(dto.getTipoArquivo());
        novoMaterial.setCaminhoArquivo(dto.getCaminhoArquivo());
        novoMaterial.setTamanhoArquivo(dto.getTamanhoArquivo());
        novoMaterial.setDisciplina(novaDisciplina); // esse é o set de disciplinas
        return materialRepository.save(novoMaterial);
    }

    public void excluir (UUID id){
        MaterialModel remover = buscarPorId(id);
        materialRepository.delete(remover);
    }
}
