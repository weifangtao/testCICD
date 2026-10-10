package com.example.testdemo.lib.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * HTTP 日志脱敏测试。
 *
 * 这些断言是「开着日志也不泄露凭证」这个安全承诺的**可执行证据**。
 */
class HttpLogSanitizerTest {

    // ── ① 敏感请求头 ──

    @Test
    fun `authorization header value is redacted`() {
        val redacted = "-> Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig".redactSensitive()

        assertFalse(redacted.contains("eyJhbGciOiJIUzI1NiJ9"), "token 不应出现在日志中")
        assertTrue(redacted.contains("Authorization"))
        assertTrue(redacted.endsWith("***"))
    }

    @Test
    fun `cookie header value is redacted`() {
        val redacted = "-> Cookie: session=abc123; uid=42".redactSensitive()

        assertFalse(redacted.contains("abc123"))
        assertFalse(redacted.contains("uid=42"))
    }

    @Test
    fun `api key header is redacted regardless of spelling`() {
        listOf("X-Api-Key", "apikey", "x-api-key", "API_KEY").forEach { name ->
            val redacted = "-> $name: super-secret-value".redactSensitive()
            assertFalse(redacted.contains("super-secret-value"), "$name 的值应被脱敏")
        }
    }

    @Test
    fun `authorization header is redacted case insensitively`() {
        val redacted = "-> AUTHORIZATION: Basic dXNlcjpwYXNz".redactSensitive()
        assertFalse(redacted.contains("dXNlcjpwYXNz"))
    }

    // ── ② 正常内容不受影响 ──

    @Test
    fun `non sensitive headers are preserved`() {
        val log = "-> Accept: application/json"
        assertEquals(log, log.redactSensitive())
    }

    @Test
    fun `request line and status are preserved`() {
        val log = "REQUEST: https://api.example.com/posts\nRESPONSE: 200 OK"
        assertEquals(log, log.redactSensitive())
    }

    // ── ③ URL 查询参数 ──

    @Test
    fun `token query parameter is redacted`() {
        val redacted = "REQUEST: https://api.example.com/user?token=abc123&page=2".redactSensitive()

        assertFalse(redacted.contains("abc123"))
        assertTrue(redacted.contains("page=2"), "非敏感参数应保留")
    }

    @Test
    fun `snake case token parameters are redacted`() {
        val redacted =
            "https://x.com/a?access_token=aaa&refresh_token=bbb&api_key=ccc".redactSensitive()

        assertFalse(redacted.contains("aaa"))
        assertFalse(redacted.contains("bbb"))
        assertFalse(redacted.contains("ccc"))
    }

    // ── ④ URL 内嵌 Basic Auth ──

    @Test
    fun `credentials embedded in url are redacted`() {
        val redacted = "REQUEST: https://admin:p%40ss@internal.example.com/api".redactSensitive()

        assertFalse(redacted.contains("admin"))
        assertFalse(redacted.contains("p%40ss"))
        assertTrue(redacted.contains("internal.example.com"), "域名应保留，便于定位问题")
    }

    // ── ⑤ 响应体敏感字段 ──

    @Test
    fun `token fields in json body are redacted`() {
        val redacted =
            """{"code":0,"access_token":"eyJhbGci","user":{"name":"张三"}}""".redactSensitive()

        assertFalse(redacted.contains("eyJhbGci"))
        assertTrue(redacted.contains("张三"), "非敏感字段应保留，便于排查")
    }

    @Test
    fun `password field in json body is redacted`() {
        val redacted = """{"password":"hunter2"}""".redactSensitive()
        assertFalse(redacted.contains("hunter2"))
    }

    // ── ⑥ 组合场景 ──

    @Test
    fun `full request dump leaks nothing sensitive`() {
        val dump = """
            REQUEST: https://api.example.com/login?token=urltoken
            METHOD: HttpMethod(value=POST)
            COMMON HEADERS
            -> Accept: application/json
            -> Authorization: Bearer headerToken
            -> Cookie: session=cookieToken
            BODY Content-Type: application/json
            {"username":"zhangsan","password":"pw123456","access_token":"bodyToken"}
        """.trimIndent()

        val redacted = dump.redactSensitive()

        listOf("urltoken", "headerToken", "cookieToken", "pw123456", "bodyToken").forEach { secret ->
            assertFalse(redacted.contains(secret), "「$secret」不应出现在日志中")
        }
        // 排查问题需要的信息仍在
        assertTrue(redacted.contains("https://api.example.com/login"))
        assertTrue(redacted.contains("zhangsan"))
    }
}
