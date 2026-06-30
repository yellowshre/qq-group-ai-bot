package com.yh.qqbot.exception;

import com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException;
import com.yh.qqbot.chat.history.service.InvalidChatHistoryFilePathException;
import com.yh.qqbot.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            InvalidChatCandidateRequestException.class,
            InvalidChatHistoryFilePathException.class,
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            BindException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST", badRequestMessage(ex)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handle(Exception ex) {
        log.warn("Unhandled web exception.", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "internal error"));
    }

    private String badRequestMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException validException) {
            FieldError fieldError = validException.getBindingResult().getFieldError();
            if (fieldError != null && hasText(fieldError.getDefaultMessage())) {
                return fieldError.getDefaultMessage();
            }
            return "invalid request";
        }
        if (ex instanceof BindException bindException) {
            FieldError fieldError = bindException.getBindingResult().getFieldError();
            if (fieldError != null && hasText(fieldError.getDefaultMessage())) {
                return fieldError.getDefaultMessage();
            }
            return "invalid request";
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "invalid request body";
        }
        if (ex instanceof MissingServletRequestParameterException missingParameterException) {
            return missingParameterException.getParameterName() + " is required";
        }
        return shortMessage(ex);
    }

    private String shortMessage(Exception ex) {
        String message = ex.getMessage();
        if (!hasText(message)) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
