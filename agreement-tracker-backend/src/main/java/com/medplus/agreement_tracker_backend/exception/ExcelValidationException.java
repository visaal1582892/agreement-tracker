package com.medplus.agreement_tracker_backend.exception;

public class ExcelValidationException extends RuntimeException {

    private final byte[] errorWorkbook;

    public ExcelValidationException(byte[] errorWorkbook) {
        super("JBP workbook validation failed. See the annotated error file for details.");
        this.errorWorkbook = errorWorkbook;
    }

    public byte[] getErrorWorkbook() {
        return errorWorkbook;
    }
}
