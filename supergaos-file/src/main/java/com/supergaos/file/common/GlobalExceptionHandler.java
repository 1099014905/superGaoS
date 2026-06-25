package com.supergaos.file.common;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for file-service.
 * Catches all unhandled exceptions and returns a consistent Result&lt;T&gt; response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles known business exceptions (e.g. file not found, upload failure).
     * Returns the specific error code and message defined in the BusinessException.
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getErrorCode(), e.getMessage());
    }

    /**
     * Handles all unexpected runtime exceptions as a generic 500 error.
     * Forwards the error message for debugging without exposing stack traces.
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.error(500, "Internal Server Error");
    }
}
