package com.example.testdemo.auth

import com.example.testdemo.lib.config.AppEnvironment
import com.example.testdemo.lib.config.AppEnvironmentConfigHolder
import com.example.testdemo.lib.storage.AppStorage
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionManagerTest {
    private val values = mutableMapOf<String, String?>()
    private val storage = mockk<AppStorage>()
    private fun manager() = SessionManager(storage, { "encrypted:$it" }, { it.removePrefix("encrypted:") })
    private fun session(user: String = "alice", environment: String = "test") = Session(environment, LoginUser("token-$user", user))

    @Before fun setup() {
        AppEnvironmentConfigHolder.initialize(AppEnvironment.TEST)
        coEvery { storage.getString(any(), any()) } answers { values[firstArg()] }
        coEvery { storage.putString(any(), any()) } answers { values[firstArg()] = secondArg(); Unit }
        coEvery { storage.remove(any()) } answers { values.remove(firstArg()); Unit }
    }
    @Test fun `restore starts logged out then restores encrypted session`() = runTest {
        val first = manager()
        first.restore()
        assertTrue(first.state.value.ready)
        assertNull(first.state.value.session)
        assertTrue(first.accept(0, session()))
        assertTrue(values.getValue("session")!!.startsWith("encrypted:"))
        val second = manager()
        second.restore()
        assertEquals(session(), second.state.value.session)
    }
    @Test fun `logout survives process recreation`() = runTest {
        val first = manager()
        first.restore(); first.accept(0, session()); first.logout()
        val second = manager(); second.restore()
        assertNull(second.state.value.session)
        assertNull(values["session"])
    }
    @Test fun `environment switch invalidates token and late login result`() = runTest {
        val manager = manager(); manager.restore()
        val old = manager.state.value.generation
        manager.switchEnvironment(AppEnvironment.PROD)
        assertFalse(manager.accept(old, session()))
        assertNull(manager.state.value.session)
        assertEquals(AppEnvironment.PROD, AppEnvironmentConfigHolder.current())
    }
    @Test fun `old unauthorized response cannot clear newer session`() = runTest {
        val manager = manager(); manager.restore(); manager.accept(0, session())
        val old = manager.state.value.generation
        manager.logout()
        manager.accept(manager.state.value.generation, session("bob"))
        manager.logout("expired", old)
        assertEquals("bob", manager.state.value.session?.user?.userCode)
    }
    @Test fun `corrupt persisted session safely returns to login`() = runTest {
        values["session"] = "broken-json"
        val manager = manager(); manager.restore()
        assertTrue(manager.state.value.ready)
        assertNull(manager.state.value.session)
        assertNotNull(manager.state.value.message)
    }
    @Test fun `restored token from different environment is not used`() = runTest {
        val manager = manager(); manager.restore(); manager.accept(0, session())
        AppEnvironmentConfigHolder.initialize(AppEnvironment.PROD)
        val next = manager(); next.restore()
        assertNull(next.state.value.session)
    }
    @Test fun `cache scope separates accounts environments and warehouses`() {
        val first = session()
        assertNotEquals(first.scopeKey, session("bob").scopeKey)
        assertNotEquals(first.scopeKey, session(environment = "prod").scopeKey)
        assertNotEquals(first.scopeKey, first.copy(user = first.user.copy(houseId = "warehouse-2")).scopeKey)
        assertFalse(first.scopeKey.contains("alice"))
    }
    @Test fun `failed persistence does not publish authenticated state`() = runTest {
        coEvery { storage.putString(any(), any()) } throws java.io.IOException("disk full")
        val manager = manager(); manager.restore()
        try { manager.accept(0, session()); fail("must fail") } catch (_: java.io.IOException) { }
        assertNull(manager.state.value.session)
    }
}
