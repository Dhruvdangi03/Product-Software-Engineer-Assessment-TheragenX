package com.theragenx.intake.catalog;

/** The catalog file is missing, unreadable or inconsistent. */
public class CatalogException extends RuntimeException {

    public CatalogException(String message) {
        super(message);
    }

    public CatalogException(String message, Throwable cause) {
        super(message, cause);
    }
}
