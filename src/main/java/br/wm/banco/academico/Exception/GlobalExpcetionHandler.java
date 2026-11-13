package br.wm.banco.academico.Exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExpcetionHandler {
        @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleParametroInvalido() {
            return ResponseEntity.badRequest().body(new ErrorResponse("Parâmetro inválido.", 400));
        }
        @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
        public ResponseEntity<ErrorResponse> handleEdicaoConcorrente() {
            return ResponseEntity.status(409).body(new ErrorResponse("O conteúdo mudou. Atualize a página antes de editar.", 409));
        }
        @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleIntegridade() {
            return ResponseEntity.status(409).body(new ErrorResponse("A operação conflita com registros vinculados. Atualize a página e tente novamente.", 409));
        }
        @ExceptionHandler({org.springframework.web.multipart.support.MissingServletRequestPartException.class,
                org.springframework.http.converter.HttpMessageNotReadableException.class,
                org.springframework.web.multipart.MultipartException.class})
        public ResponseEntity<ErrorResponse> handleMultipartInvalido(Exception ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Requisição inválida. Envie os dados e o arquivo nos campos esperados.", 400));
        }
        @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<ErrorResponse> handleTipoInvalido() {
            return ResponseEntity.status(415).body(new ErrorResponse("Tipo de conteúdo não suportado.", 415));
        }
        @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
        public ResponseEntity<ErrorResponse> handleUploadMuitoGrande() {
            return ResponseEntity.status(413).body(new ErrorResponse("O arquivo excede o tamanho permitido.", 413));
        }
        @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
        public ResponseEntity<ErrorResponse> handleStatus(org.springframework.web.server.ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                .body(new ErrorResponse(ex.getReason(), ex.getStatusCode().value()));
        }


        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handlerNotFound(ResourceNotFoundException ex){
            ErrorResponse error =  new ErrorResponse(ex.getMessage(), 404);
            return ResponseEntity.status(404).body(error);
        }
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

            String mensagem = ex.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(erro -> erro.getDefaultMessage())
                    .findFirst()
                    .orElse("Dados inválidos!");

            ErrorResponse error = new ErrorResponse(mensagem, 400);

            return ResponseEntity.status(400).body(error);
        }

        @ExceptionHandler(RegraNegocioException.class)
        public ResponseEntity<ErrorResponse> handleRegraNegocio(RegraNegocioException ex) {

            ErrorResponse error = new ErrorResponse(ex.getMessage(), 409);

            return ResponseEntity.status(409).body(error);
        }
}
