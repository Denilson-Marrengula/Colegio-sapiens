package mz.colegiosapi.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField() + ": " + e.getDefaultMessage()).collect(Collectors.toList());
        return ResponseEntity.badRequest().body(Map.of("error", "Dados inválidos", "detalhes", errors));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Registo duplicado ou referenciado por outro registo"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getReason() == null ? "Erro" : ex.getReason()));
    }
}
