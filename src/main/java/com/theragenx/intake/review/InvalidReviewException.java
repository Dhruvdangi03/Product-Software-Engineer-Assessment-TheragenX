package com.theragenx.intake.review;

/** A review request that cannot be applied, for example an edited value that fails the catalog. */
public class InvalidReviewException extends RuntimeException {

    public InvalidReviewException(String message) {
        super(message);
    }
}
