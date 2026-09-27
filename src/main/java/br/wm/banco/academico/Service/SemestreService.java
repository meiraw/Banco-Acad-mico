package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Request.SemestreRequestDTO;
import br.wm.banco.academico.Exception.ResourceNotFoundException;
import br.wm.banco.academico.Model.SemestreModel;
import br.wm.banco.academico.Repository.DisciplinasRepository;
import br.wm.banco.academico.Repository.SemestreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SemestreService {

    private final SemestreRepository semestreRepository;

    public SemestreService (SemestreRepository semestreRepository){
        this.semestreRepository = semestreRepository;
    }


    public SemestreModel criar (SemestreRequestDTO dto ){
        SemestreModel semestre = new SemestreModel();

        semestre.setAno(dto.getAno());
        semestre.setPeriodo(dto.getPeriodo());

        return semestreRepository.save(semestre);
    }

    public Page<SemestreModel> listar (Pageable pageable ){
        return semestreRepository.findAll(pageable );
    }

    public SemestreModel buscarPorId(UUID id){
        return semestreRepository.findById(id).orElseThrow( () -> new ResourceNotFoundException("O id "+id+"não foi encontrado!"));
    }

    public SemestreModel atualizar (SemestreRequestDTO dto, UUID id){

        SemestreModel novoSemestre = buscarPorId(id);

        novoSemestre.setAno(dto.getAno());
        novoSemestre.setPeriodo(dto.getPeriodo());

        return semestreRepository.save(novoSemestre);
    }

    public void excluir (UUID id){
        SemestreModel deletar = buscarPorId(id);
        semestreRepository.delete(deletar);
    }

}
