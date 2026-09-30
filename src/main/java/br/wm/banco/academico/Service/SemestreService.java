package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Request.SemestreRequestDTO;
import br.wm.banco.academico.Exception.ResourceNotFoundException;
import br.wm.banco.academico.Model.CursoModel;
import br.wm.banco.academico.Model.SemestreModel;
import br.wm.banco.academico.Repository.SemestreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SemestreService {

    private final SemestreRepository semestreRepository;
    private final CursoService cursoService;

    public SemestreService (SemestreRepository semestreRepository, CursoService cursoService){
        this.semestreRepository = semestreRepository;
        this.cursoService = cursoService;
    }


    public SemestreModel criar (SemestreRequestDTO dto  ){

        CursoModel curso = cursoService.buscarPorId(dto.getCursoId());

        SemestreModel semestre = new SemestreModel();
        semestre.setNumero(dto.getNumero());
        semestre.setNome(dto.getNome());
        semestre.setCurso(curso);
        return semestreRepository.save(semestre);
    }

    public List<SemestreModel> listar() {
        return semestreRepository.findAll();
    }

    public SemestreModel buscarPorId(UUID id){
        return semestreRepository.findById(id).orElseThrow( () -> new ResourceNotFoundException("O id "+id+"não foi encontrado!"));
    }

    public SemestreModel atualizar (SemestreRequestDTO dto, UUID id){

        CursoModel curso = cursoService.buscarPorId(dto.getCursoId());

        SemestreModel novoSemestre = buscarPorId(id);

        novoSemestre.setNumero(dto.getNumero());
        novoSemestre.setNome(dto.getNome());
        novoSemestre.setCurso(curso);

        return semestreRepository.save(novoSemestre);
    }

    public void excluir (UUID id){
        SemestreModel deletar = buscarPorId(id);
        semestreRepository.delete(deletar);
    }

}
