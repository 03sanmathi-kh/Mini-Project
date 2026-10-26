package edu.kle.permits;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Dtos.ErrorBody> invalid(MethodArgumentNotValidException e){String detail=e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getField()+": "+x.getDefaultMessage()).orElse("Check all fields.");return ResponseEntity.badRequest().body(new Dtos.ErrorBody(detail));}
 @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class) ResponseEntity<Dtos.ErrorBody> badJson(){return ResponseEntity.badRequest().body(new Dtos.ErrorBody("Invalid request value."));}
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<Dtos.ErrorBody> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(new Dtos.ErrorBody(e.getReason()));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<Dtos.ErrorBody> conflict(){return ResponseEntity.status(409).body(new Dtos.ErrorBody("Conflicting data. The email or record must be unique."));}
}
