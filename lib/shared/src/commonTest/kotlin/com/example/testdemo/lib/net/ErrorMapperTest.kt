package com.example.testdemo.lib.net

import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 错误映射测试（跨端 `commonTest`：Android 与 iOS 都会跑同一份断言）。
 */
class ErrorMapperTest {

    @Test
    fun `timeout maps to TIMEOUT`() {
        val error = ErrorMapper.fromThrowable(SocketTimeoutException("timeout"))
        assertEquals(ApiErrorType.TIMEOUT, error.type)
    }

    @Test
    fun `serialization error maps to PARSE`() {
        val error = ErrorMapper.fromThrowable(SerializationException("bad json"))
        assertEquals(ApiErrorType.PARSE, error.type)
    }

    @Test
    fun `io error maps to CONNECT`() {
        val error = ErrorMapper.fromThrowable(IOException("connect failed"))
        assertEquals(ApiErrorType.CONNECT, error.type)
    }

    @Test
    fun `unknown throwable falls back to UNKNOWN`() {
        val error = ErrorMapper.fromThrowable(IllegalStateException("boom"))
        assertEquals(ApiErrorType.UNKNOWN, error.type)
        assertEquals("boom", error.message)
    }

    @Test
    fun `http error keeps status code`() {
        val error = ErrorMapper.fromHttp(500, null)
        assertEquals(ApiErrorType.HTTP, error.type)
        assertEquals("500", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `http error prefers server body as message`() {
        assertEquals("参数错误", ErrorMapper.fromHttp(400, "参数错误").message)
    }

    @Test
    fun `blank server body falls back to default message`() {
        assertTrue(ErrorMapper.fromHttp(400, "   ").message.isNotBlank())
    }

    @Test
    fun `envelope failure maps to BUSINESS with backend message and code`() {
        val error = ErrorMapper.fromEnvelope(
            ApiEnvelope<Any>(result = false, message = "库存不足", code = "1001")
        )
        assertEquals(ApiErrorType.BUSINESS, error.type)
        assertEquals("库存不足", error.message)
        assertEquals("1001", error.code)
    }

    @Test
    fun `null envelope maps to EMPTY_BODY`() {
        assertEquals(ApiErrorType.EMPTY_BODY, ErrorMapper.fromEnvelope(null).type)
    }

    @Test
    fun `api result failure exposes message`() {
        val result: ApiResult<String> = ApiResult.Failure(ApiError.connect())
        assertEquals(ApiError.connect().message, result.errorMessage())
    }

    @Test
    fun `api result success maps data`() {
        val result = ApiResult.Success(2).map { it * 3 }
        assertEquals(6, result.getOrNull())
    }
}
