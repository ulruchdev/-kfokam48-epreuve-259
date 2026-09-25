package com.kfokam48.attendance.web.erreur;

import org.springframework.http.HttpStatus;

/**
 * Base of all business exceptions. Each carries the stable uppercase error code
 * required by the contract and the HTTP status it maps to (B2).
 * The French message is imposed by the contract's error format.
 */
public abstract class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    protected ApiException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
