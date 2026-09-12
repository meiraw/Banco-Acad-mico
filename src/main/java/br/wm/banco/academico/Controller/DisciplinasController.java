package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Request.DisciplinasRequestDTO;
import br.wm.banco.academico.DTOs.Response.DisciplinasResponseDTO;
import br.wm.banco.academico.Model.DisciplinasModel;
import br.wm.banco.academico.Service.DisciplinasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Disciplinas")
public class DisciplinasController {

    private final DisciplinasService disciplinasService;

    public DisciplinasController(DisciplinasService disciplinasService) {
        this.disciplinasService = disciplinasService;
    }
}


