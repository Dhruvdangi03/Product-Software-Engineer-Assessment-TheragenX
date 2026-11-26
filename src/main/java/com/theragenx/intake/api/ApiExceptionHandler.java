package com.theragenx.intake.api;

import com.theragenx.intake.review.InvalidReviewException;
import com.theragenx.intake.review.ReviewTargetNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns our exceptions into RFC 7807 problem responses with a message the UI can show. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({DocumentNotFoundException.class, ReviewTargetNotFoundException.class})
    public ProblemDetail notFound(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidReviewException.class)
    public ProblemDetail invalidReview(InvalidReviewException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadableBody(HttpMessageNotReadableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "The request body is not valid: " + e.getMostSpecificCause().getMessage());
    }
}
