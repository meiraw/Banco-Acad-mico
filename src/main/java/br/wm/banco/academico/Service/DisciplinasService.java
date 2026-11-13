package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Request.DisciplinasRequestDTO;

import br.wm.banco.academico.Exception.ResourceNotFoundException;
import br.wm.banco.academico.Model.DisciplinasModel;
import br.wm.banco.academico.Model.SemestreModel;
import br.wm.banco.academico.Repository.DisciplinasRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.util.UUID;

@Service
public class DisciplinasService {




    //Injeção de dependencia , subtituimos o @Autowired pelo metodo
    private final DisciplinasRepository disciplinasRepository;
    private final SemestreService semestreService;
    private final br.wm.banco.academico.Repository.DiscussaoRepository discussoes;
    public DisciplinasService(
    DisciplinasRepository disciplinasRepository,SemestreService semestreService,
    br.wm.banco.academico.Repository.DiscussaoRepository discussoes){
        this.disciplinasRepository = disciplinasRepository;
        this.semestreService = semestreService;
        this.discussoes = discussoes;
    }



    @Transactional
    public DisciplinasModel criar (DisciplinasRequestDTO dto){
        DisciplinasModel disciplinas = new DisciplinasModel();
        SemestreModel semestre = semestreService.buscarPorId(dto.getSemestreId());

        disciplinas.setNome(dto.getNome());
        disciplinas.setDescricao(dto.getDescricao());
        disciplinas.setSemestre(semestre);
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
        SemestreModel semestre = semestreService.buscarPorId(dto.getSemestreId());

        novaDisciplina.setNome(dto.getNome());
        novaDisciplina.setDescricao(dto.getDescricao());
        novaDisciplina.setSemestre(semestre);

        return disciplinasRepository.save(novaDisciplina);
    }

    @Transactional
    public void excluir (UUID id){
        DisciplinasModel deletar = buscarPorId(id);
        if (discussoes.existsByDisciplinaId(id))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                "Esta disciplina possui histórico no fórum e não pode ser excluída.");
        disciplinasRepository.delete(deletar);
    }
}
