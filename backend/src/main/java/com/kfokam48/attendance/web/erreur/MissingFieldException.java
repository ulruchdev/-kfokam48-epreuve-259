package com.kfokam48.attendance.web.erreur;

import org.springframework.http.HttpStatus;

/** Generic 400 for cross-field or range validation failures. */
public class MissingFieldException extends ApiException {
    public MissingFieldException(String message) {
        super("CHAMP_MANQUANT", HttpStatus.BAD_REQUEST, message);
    }
}
