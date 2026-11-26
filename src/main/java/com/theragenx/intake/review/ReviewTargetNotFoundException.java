package com.theragenx.intake.review;

/** The document, case or field a review refers to does not exist. */
public class ReviewTargetNotFoundException extends RuntimeException {

    public ReviewTargetNotFoundException(String message) {
        super(message);
    }
}
