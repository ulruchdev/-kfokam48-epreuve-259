package com.kfokam48.attendance.web.erreur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * B4 + B2: every error — without exception — is returned in the imposed format
 * {"code": "MAJUSCULES", "message": "phrase française"}. No stack trace ever
 * reaches the client (server.error.include-stacktrace: never + handler below).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ApiError(String code, String message) {}

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> onApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getCode(), ex.getMessage()));
    }

    /** Bean Validation failures (@Valid) → 400 CHAMP_MANQUANT. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> onValidation(MethodArgumentNotValidException ex) {
        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        return ResponseEntity.badRequest()
                .body(new ApiError("CHAMP_MANQUANT", "Champ(s) manquant(s) ou invalide(s) : " + fields));
    }

    /** Malformed JSON body → 400 CHAMP_MANQUANT. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> onUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("CHAMP_MANQUANT", "Corps de la requête manquant ou mal formé."));
    }

    /** Bad path/query parameter types → 400 CHAMP_MANQUANT. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> onTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("CHAMP_MANQUANT", "Paramètre invalide : " + ex.getName()));
    }

    /** Unknown route → 404 in the imposed format. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> onNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("RESSOURCE_INCONNUE", "Ressource inconnue."));
    }

    /** Last-resort guard: even unexpected errors match the imposed format. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> onUnexpected(Exception ex) {
        log.error("Erreur inattendue", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("ERREUR_INTERNE", "Une erreur interne est survenue."));
    }
}
