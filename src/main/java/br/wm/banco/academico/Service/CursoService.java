package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Request.CursoRequestDTO;
import br.wm.banco.academico.Exception.ResourceNotFoundException;
import br.wm.banco.academico.Model.CursoModel;
import br.wm.banco.academico.Repository.CursoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;

    public CursoService(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    public CursoModel criar(CursoRequestDTO dto) {

        CursoModel curso = new CursoModel();
        curso.setNome(dto.getNome());

        return cursoRepository.save(curso);
    }

    public List<CursoModel> listar() {
        return cursoRepository.findAll();
    }

    public CursoModel buscarPorId(UUID id) {
        return cursoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("O id " + id + " não foi encontrado!"));
    }

    public CursoModel atualizar(CursoRequestDTO dto, UUID id) {

        CursoModel novocurso = buscarPorId(id);
        novocurso.setNome(dto.getNome());
        return cursoRepository.save(novocurso);
    }

    public void deletar(UUID id) {
        CursoModel excluir = buscarPorId(id);
        cursoRepository.delete(excluir);
    }

}

