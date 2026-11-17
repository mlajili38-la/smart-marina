package tn.iset.marina.consumption_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tn.iset.marina.consumption_service.dto.ErrorResponse;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ConsumptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(PedestalNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePedestalNotFound(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request,
                List.of(new ErrorResponse.FieldError("pedestalId", exception.getMessage())));
    }

    @ExceptionHandler(PedestalNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleNotActive(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ForbiddenPortAccessException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(PedestalServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> new ErrorResponse.FieldError(field.getField(), field.getDefaultMessage()))
                .toList();
        return error(HttpStatus.BAD_REQUEST, "La requête contient des champs invalides", request, fields);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleUnknownSort(
            PropertyReferenceException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Un paramètre de la requête est invalide", request,
                List.of(new ErrorResponse.FieldError("sort", "Champ de tri inconnu : " + exception.getPropertyName())));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Requête invalide ou mal formée", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        logger.error("Erreur inattendue pendant le traitement de {}", request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue", request, List.of());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request,
                                                List<ErrorResponse.FieldError> fieldErrors) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
