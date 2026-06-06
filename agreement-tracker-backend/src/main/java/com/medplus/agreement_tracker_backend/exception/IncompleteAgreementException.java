package com.medplus.agreement_tracker_backend.exception;

public class IncompleteAgreementException extends RuntimeException {

    public IncompleteAgreementException(String message) {
        super(message);
    }
}
