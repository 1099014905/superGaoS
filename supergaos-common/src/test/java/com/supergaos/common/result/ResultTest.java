package com.supergaos.common.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void successWithData_shouldReturnCode200AndMessageSuccess() {
        Result<String> result = Result.success("hello");

        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("hello", result.getData());
    }

    @Test
    void successWithoutData_shouldReturnCode200AndNullData() {
        Result<Void> result = Result.success();

        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void error_shouldReturnGivenCodeAndMessageWithNullData() {
        @SuppressWarnings("rawtypes")
        Result result = Result.error(5001, "用户名或密码错误");

        assertEquals(5001, result.getCode());
        assertEquals("用户名或密码错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void error_shouldAcceptDifferentErrorCodes() {
        Result result = Result.error(4001, "文件上传失败");

        assertEquals(4001, result.getCode());
        assertEquals("文件上传失败", result.getMessage());
    }

    @Test
    void result_shouldSupportAllDataTypes() {
        Result<Integer> intResult = Result.success(42);
        assertEquals(42, intResult.getData());

        Result<Boolean> boolResult = Result.success(true);
        assertTrue(boolResult.getData());
    }
}
