package com.supergaos.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructor_shouldSetErrorCodeAndMessage() {
        BusinessException ex = new BusinessException(5001, "用户名或密码错误");

        assertEquals(5001, ex.getErrorCode());
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void constructor_shouldBeRuntimeException() {
        BusinessException ex = new BusinessException(4001, "文件上传失败");

        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void notFound_shouldGenerateModuleScopedErrorCode() {
        BusinessException ex = BusinessException.notFound(4, "文件");

        assertEquals(4001, ex.getErrorCode());  // 4 * 1000 + 1
        assertEquals("文件不存在", ex.getMessage());
    }

    @Test
    void notFound_shouldWorkForDifferentModules() {
        BusinessException ex = BusinessException.notFound(5, "用户");

        assertEquals(5001, ex.getErrorCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void exception_shouldBeThrownAndCaught() {
        try {
            throw new BusinessException(9999, "测试异常");
        } catch (BusinessException e) {
            assertEquals(9999, e.getErrorCode());
            assertEquals("测试异常", e.getMessage());
        }
    }
}
