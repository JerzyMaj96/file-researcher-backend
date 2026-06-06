package com.jerzymaj.file_researcher_backend.exceptions;

import jakarta.mail.MessagingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;

@ControllerAdvice
public class CustomizedResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ErrorDetails> handleAllExceptions(Exception ex, WebRequest request) {

        return buildResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ExistingUserException.class)
    public final ResponseEntity<ErrorDetails> handleExistingUserException(ExistingUserException ex, WebRequest request) {

        return buildResponse(ex, request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public final ResponseEntity<ErrorDetails> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {

        return buildResponse(ex, request, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PathNotFoundException.class)
    public final ResponseEntity<ErrorDetails> handlePathNotFoundException(PathNotFoundException ex, WebRequest request) {

        return buildResponse(ex, request, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoFilesSelectedException.class)
    public final ResponseEntity<ErrorDetails> handleNoFilesSelectedException(NoFilesSelectedException ex, WebRequest request) {

        return buildResponse(ex, request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileSetNotFoundException.class)
    public final ResponseEntity<ErrorDetails> handleFileSetNotFoundException(FileSetNotFoundException ex, WebRequest request) {

        return buildResponse(ex, request, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ZipArchiveNotFoundException.class)
    public final ResponseEntity<ErrorDetails> handleZipArchiveNotFoundException(ZipArchiveNotFoundException ex, WebRequest request) {

        return buildResponse(ex, request, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SentHistoryNotFoundException.class)
    public final ResponseEntity<ErrorDetails> handleSentHistoryNotFoundException(SentHistoryNotFoundException ex, WebRequest request) {

        return buildResponse(ex, request, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public final ResponseEntity<ErrorDetails> handleAccessDeniedExceptionException(AccessDeniedException ex, WebRequest request) {

        return buildResponse(ex, request, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MessagingException.class)
    public final ResponseEntity<ErrorDetails> handleMessagingException(MessagingException ex, WebRequest request) {

        return buildResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IOException.class)
    public final ResponseEntity<ErrorDetails> handleIOException(IOException ex, WebRequest request) {

        return buildResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorDetails> buildResponse(Exception ex, WebRequest request, HttpStatus status) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity<>(errorDetails, status);
    }
}
