package com.kfokam48.attendance.web.erreur;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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

    /**
     * Missing or malformed JSON body → 400 CORPS_INVALIDE; a non-integer grade
     * (e.g. 12.5 or "abc") → 400 NOTE_INVALIDE (RG8).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> onUnreadable(HttpMessageNotReadableException ex) {
        if (isInvalidGrade(ex)) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("NOTE_INVALIDE", "La note doit être un entier entre 0 et 20."));
        }
        return ResponseEntity.badRequest()
                .body(new ApiError("CORPS_INVALIDE", "Corps de la requête manquant ou mal formé."));
    }

    private boolean isInvalidGrade(HttpMessageNotReadableException ex) {
        return ex.getCause() instanceof JsonMappingException mapping
                && mapping.getPath().stream().anyMatch(ref -> "note".equals(ref.getFieldName()));
    }

    /** Path/query parameter of the wrong type → 400 PARAMETRE_INVALIDE. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> onTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("PARAMETRE_INVALIDE", "Paramètre invalide : " + ex.getName()));
    }

    /** Verb not supported on this route → 405 METHODE_NON_AUTORISEE. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> onMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiError("METHODE_NON_AUTORISEE", "Méthode non autorisée : " + ex.getMethod()));
    }

    /** Body that is not JSON → 415 TYPE_NON_SUPPORTE. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> onUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ApiError("TYPE_NON_SUPPORTE", "Le corps de la requête doit être au format JSON."));
    }

    /** Missing required query parameter → 400 PARAMETRE_MANQUANT (never a 500). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> onMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("PARAMETRE_MANQUANT", "Paramètre manquant : " + ex.getParameterName()));
    }

    /** Unknown route → 404 in the imposed format. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> onNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("RESSOURCE_INCONNUE", "Ressource inconnue."));
    }

    /** A constraint violation that escaped a service (concurrent write) → 409, never a 500 (#59). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> onConstraintViolation(DataIntegrityViolationException ex) {
        log.warn("Conflit d'écriture concurrente", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("CONFLIT_CONCURRENT",
                        "La ressource a été modifiée au même moment par une autre requête, réessayez."));
    }

    /** Last-resort guard: even unexpected errors match the imposed format. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> onUnexpected(Exception ex) {
        log.error("Erreur inattendue", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("ERREUR_INTERNE", "Une erreur interne est survenue."));
    }
}
