package com.example.customermanagementservice.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions into JSON error responses (RFC 9457 "problem details").
 * Spring's own errors (bad JSON, unknown URL, wrong HTTP method...) are handled by
 * {@link ResponseEntityExceptionHandler}; their Turkish texts are in messages.properties.
 */
@RestControllerAdvice
public class GeneralExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GeneralExceptionHandler.class);

    @ExceptionHandler(CustomerNotFoundException.class)
    public ProblemDetail handleCustomerNotFound(CustomerNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DuplicateCitizenNumberException.class)
    public ProblemDetail handleDuplicateCitizenNumber(DuplicateCitizenNumberException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidSortPropertyException.class)
    public ProblemDetail handleInvalidSortProperty(InvalidSortPropertyException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    // Safety net for the database unique constraint (e.g. two requests adding the same customer at once).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.warn("Data integrity violation", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "İstek mevcut kayıtlarla çakışıyor.");
    }

    // Adds a field -> message map so a form can show which input is wrong.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail body = exception.getBody();
        body.setDetail("Gönderilen bilgilerde hata var.");
        body.setProperty("errors", errors);
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    // Anything unexpected: log the details, but don't send internal messages to the client.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata oluştu.");
    }
}
