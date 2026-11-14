package com.nameless.nasa.infrastructure.input.exception;

import com.nameless.nasa.domain.exception.EmailAlreadyRegisteredException;
import com.nameless.nasa.domain.exception.InvalidCredentialsException;
import com.nameless.nasa.domain.exception.PhoneAlreadyRegisteredException;
import com.nameless.nasa.domain.exception.PolicyNotAcceptedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({PhoneAlreadyRegisteredException.class, EmailAlreadyRegisteredException.class,
            DataIntegrityViolationException.class})
    ProblemDetail handleConflict(Exception ex) {
        String detail = ex instanceof DataIntegrityViolationException
                ? "Phone or email is already registered" : ex.getMessage();
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
    }

    @ExceptionHandler(PolicyNotAcceptedException.class)
    ProblemDetail handlePolicy(PolicyNotAcceptedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
