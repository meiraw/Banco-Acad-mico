package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Request.DisciplinasRequestDTO;
import br.wm.banco.academico.DTOs.Response.DisciplinasResponseDTO;
import br.wm.banco.academico.Exception.ResourceNotFoundException;
import br.wm.banco.academico.Model.DisciplinasModel;
import br.wm.banco.academico.Repository.DisciplinasRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.util.UUID;

@Service
public class DisciplinasService {


    //Injeção de dependencia , subtituimos o @Autowired pelo metodo
    private final DisciplinasRepository disciplinasRepository;
    public DisciplinasService(
    DisciplinasRepository disciplinasRepository){
        this.disciplinasRepository = disciplinasRepository;
    }

    @Transactional
    public DisciplinasModel criar (DisciplinasRequestDTO dto){
        DisciplinasModel disciplinas = new DisciplinasModel();

        disciplinas.setNome(dto.getNome());
        disciplinas.setDescricao(dto.getDescricao());
        disciplinas.setSemestre(dto.getSemestre());

        return disciplinasRepository.save(disciplinas);
    }

    public DisciplinasModel buscarPorId (UUID id){
        return disciplinasRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("O id "+id+" não foi encontrado!"));
    }

    public Page<DisciplinasModel> listarTudo(Pageable pageable ){
        return  disciplinasRepository.findAll(pageable);
    }

    @Transactional
    public DisciplinasModel atualizar (DisciplinasRequestDTO dto, UUID id){
        DisciplinasModel novaDisciplina = buscarPorId(id);
        novaDisciplina.setNome(dto.getNome());
        novaDisciplina.setDescricao(dto.getDescricao());
        novaDisciplina.setSemestre(dto.getSemestre());
        return disciplinasRepository.save(novaDisciplina);
    }

    @Transactional
    public void excluir (UUID id){
        DisciplinasModel deletar = buscarPorId(id);
        disciplinasRepository.delete(deletar);
    }
}
