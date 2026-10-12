package com.example.testdemo.auth

import com.example.testdemo.lib.config.AppEnvironment
import com.example.testdemo.lib.config.AppEnvironmentConfigHolder
import com.example.testdemo.lib.net.*
import com.example.testdemo.lib.storage.AppStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WmsGatewayTest {
    private val storage = mockk<AppStorage>(relaxed = true)
    private val sessions = SessionManager(storage, { it }, { it })
    @Before fun setup() {
        AppEnvironmentConfigHolder.initialize(AppEnvironment.TEST)
        coEvery { storage.getString(any(), any()) } returns null
    }
    @Test fun `login sends WMS envelope defaults and no auth header`() = runTest {
        val gateway = WmsGateway(sessions) { config -> NetworkClient(config, MockEngine { request ->
            assertEquals("https://wms-test.haoqianyi.com/k3_wms/app", request.url.toString())
            assertEquals(HttpMethod.Post, request.method)
            assertNull(request.headers["SESSION_MOBILE_USER_TOKEN"])
            assertNull(request.headers[HttpHeaders.Authorization])
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals("AppLogin", body["functionCode"]?.jsonPrimitive?.content)
            val payload = body.getValue("data").jsonObject
            assertEquals(false, payload["employmentType"]?.jsonPrimitive?.boolean)
            assertEquals("encrypted-password", payload["password"]?.jsonPrimitive?.content)
            assertEquals("CN", payload["language"]?.jsonPrimitive?.content)
            respond("""{"result":true,"data":{"token":"session-token","userCode":"alice","menuList":[]}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) }
        val result = gateway.request<LoginPayload, LoginUser>("AppLogin", LoginPayload("alice", "encrypted-password", 1, "AID_test", "CN"), false)
        assertEquals("session-token", (result as ApiResult.Success).data.token)
    }
    @Test fun `business rejection is not parsed as successful login`() = runTest {
        val gateway = WmsGateway(sessions) { config -> NetworkClient(config, MockEngine {
            respond("""{"result":false,"message":"账号或密码错误","code":"LOGIN_FAILED"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) }
        val result = gateway.request<String, LoginUser>("AppLogin", "input", false)
        assertEquals("账号或密码错误", (result as ApiResult.Failure).error.message)
    }
    @Test fun `missing success data is an empty body error`() = runTest {
        val gateway = WmsGateway(sessions) { config -> NetworkClient(config, MockEngine {
            respond("""{"result":true}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) }
        val result = gateway.request<String, LoginUser>("AppLogin", "input", false)
        assertEquals(ApiErrorType.EMPTY_BODY, (result as ApiResult.Failure).error.type)
    }
    @Test fun `WMS expiry and HTTP401 clear current token`() = runTest {
        for (http401 in listOf(false, true)) {
            sessions.restore()
            sessions.accept(sessions.state.value.generation, Session("test", LoginUser("private-token", "alice")))
            val gateway = WmsGateway(sessions) { config -> NetworkClient(config, MockEngine { request ->
                assertEquals("private-token", request.headers["SESSION_MOBILE_USER_TOKEN"])
                assertNull(request.headers[HttpHeaders.Authorization])
                respond("""{"result":false,"message":"请重新登录"}""",
                    status = if (http401) HttpStatusCode.Unauthorized else HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }) }
            gateway.request<String, LoginUser>("Example", "input")
            assertNull(sessions.state.value.session)
        }
    }
    @Test fun `late expiry response does not invalidate a new login`() = runTest {
        sessions.restore(); sessions.accept(0, Session("test", LoginUser("old-token", "alice")))
        val started = CompletableDeferred<Unit>(); val response = CompletableDeferred<Unit>()
        val gateway = WmsGateway(sessions) { config -> NetworkClient(config, MockEngine {
            started.complete(Unit); response.await()
            respond("""{"result":false,"message":"请重新登录"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) }
        val request = async { gateway.request<String, LoginUser>("Example", "input") }
        started.await(); sessions.logout()
        sessions.accept(sessions.state.value.generation, Session("test", LoginUser("new-token", "bob")))
        response.complete(Unit); request.await()
        assertEquals("bob", sessions.state.value.session?.user?.userCode)
    }
    @Test fun `request cancellation is rethrown`() = runTest {
        val gateway = WmsGateway(sessions) { config -> NetworkClient(config, MockEngine { throw CancellationException("cancelled") }) }
        try { gateway.request<String, LoginUser>("AppLogin", "input", false); fail("must cancel") }
        catch (_: CancellationException) { }
    }
    @Test fun `unauthenticated business request does not hit network`() = runTest {
        val gateway = WmsGateway(sessions) { error("must not create client") }
        assertTrue(gateway.request<String, LoginUser>("Example", "input") is ApiResult.Failure)
    }
}
