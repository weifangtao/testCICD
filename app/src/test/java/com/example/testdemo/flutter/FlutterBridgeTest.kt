package com.example.testdemo.flutter

import com.example.testdemo.flutter.handlers.ScannerEventStreamHandler
import com.example.testdemo.flutter.handlers.SystemInfoBridgeHandler
import com.example.testdemo.lib.config.AppEnvironment
import com.example.testdemo.lib.config.AppEnvironmentConfigHolder
import com.example.testdemo.lib.flutter.FlutterBridgeResult
import com.example.testdemo.lib.flutter.FlutterEventStreamBridge
import com.example.testdemo.lib.flutter.FlutterMethodBridge
import com.example.testdemo.lib.mvi_testing.TestCoroutineDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlutterBridgeTest {

    @get:Rule
    val coroutineRule = TestCoroutineDispatcherRule()

    @Before
    fun setup() {
        AppEnvironmentConfigHolder.initialize(AppEnvironment.TEST)
    }

    @Test
    fun `registered MethodHandler returns system info`() = runTest {
        val bridge = FlutterMethodBridge(ioDispatcher = coroutineRule.testDispatcher)
        bridge.registerHandler(SystemInfoBridgeHandler())

        var response: FlutterBridgeResult<Any?>? = null
        bridge.onMethodCall("getSystemInfo", null) {
            response = it
        }

        testScheduler.advanceUntilIdle()

        assertTrue(response is FlutterBridgeResult.Success)
        val data = (response as FlutterBridgeResult.Success).data as Map<*, *>
        assertEquals("testDemo", data["appName"])
        assertEquals("test", data["environmentKey"])
    }

    @Test
    fun `unregistered MethodHandler returns METHOD_NOT_FOUND error`() = runTest {
        val bridge = FlutterMethodBridge(ioDispatcher = coroutineRule.testDispatcher)

        var response: FlutterBridgeResult<Any?>? = null
        bridge.onMethodCall("unknownMethod", null) {
            response = it
        }

        assertTrue(response is FlutterBridgeResult.Error)
        assertEquals("METHOD_NOT_FOUND", (response as FlutterBridgeResult.Error).code)
    }

    @Test
    fun `EventStream dispatches barcode events to listener`() = runTest {
        val eventBridge = FlutterEventStreamBridge()
        val scannerHandler = ScannerEventStreamHandler()
        eventBridge.registerStreamHandler(scannerHandler)

        var receivedEvent: Any? = null
        eventBridge.onListen(scannerHandler.streamName) { event ->
            receivedEvent = event
        }

        scannerHandler.dispatchBarcodeScan("6901234567890", "EAN_13")
        testScheduler.advanceUntilIdle()

        assertTrue(receivedEvent is Map<*, *>)
        val map = receivedEvent as Map<*, *>
        assertEquals("6901234567890", map["barcode"])
        assertEquals("EAN_13", map["scanType"])
    }
}
