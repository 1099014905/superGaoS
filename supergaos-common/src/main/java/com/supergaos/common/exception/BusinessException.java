package com.supergaos.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int errorCode;

    public BusinessException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static BusinessException notFound(int moduleCode, String resource) {
        return new BusinessException(moduleCode * 1000 + 1, resource + "不存在");
    }
}
