package com.supergaos.blog.common;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for blog-service.
 * Catches all unhandled exceptions and returns a consistent Result<T> response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles known business exceptions (e.g. article not found, validation errors).
     * Returns the specific error code and message defined in the BusinessException.
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getErrorCode(), e.getMessage());
    }

    /**
     * Handles all unexpected runtime exceptions as a generic 500 error.
     * The error message is forwarded to help with debugging, never exposes stack traces.
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.error(500, "服务器内部错误: " + e.getMessage());
    }
}
