package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Request.MaterialRequestDTO;
import br.wm.banco.academico.Model.MaterialModel;
import br.wm.banco.academico.Repository.DisciplinasRepository;
import br.wm.banco.academico.Repository.MaterialRepository;
import org.springframework.stereotype.Service;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    public MaterialService(
            MaterialRepository materialRepository){
        this.materialRepository = materialRepository;
    }

    public MaterialModel criar (MaterialRequestDTO dto ) {

        MaterialModel material = new MaterialModel();

        material.setTitulo(dto.getTitulo());
        material.setDescricao(dto.getDescricao());
        material.setNomeArquivo(dto.getNomeArquivo());
        material.setTipoArquivo(dto.getTipoArquivo());
        material.setCaminhoArquivo(dto.getCaminhoArquivo());
        material.setTamanhoArquivo(dto.getTamanhoArquivo());

        return materialRepository.save(material);
    }
}
