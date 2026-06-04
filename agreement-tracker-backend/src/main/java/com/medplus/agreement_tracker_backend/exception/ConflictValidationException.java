package com.medplus.agreement_tracker_backend.exception;

public class ConflictValidationException extends RuntimeException {

    public ConflictValidationException(String message) {
        super(message);
    }
}
