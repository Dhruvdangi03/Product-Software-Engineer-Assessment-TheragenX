package com.theragenx.intake.api;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String documentId) {
        super("No document with id " + documentId);
    }
}
