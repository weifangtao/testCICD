package com.example.testdemo.flutter.handlers

import com.example.testdemo.lib.flutter.FlutterEventEmitter
import com.example.testdemo.lib.flutter.FlutterEventStreamHandler

/**
 * 扫码枪广播 / 扫码事件实时推送到 Flutter 的 EventChannel 处理器。
 *
 * ChannelName: `com.example.testdemo/scanner_events`
 */
class ScannerEventStreamHandler : FlutterEventStreamHandler {

    override val streamName: String = "com.example.testdemo/scanner_events"

    private var currentEmitter: FlutterEventEmitter? = null

    override fun onListen(emitter: FlutterEventEmitter) {
        currentEmitter = emitter
    }

    override fun onCancel() {
        currentEmitter = null
    }

    /**
     * 当 PDA 或红外条码枪扫到数据时，由 Native 广播给 Flutter。
     *
     * @param barcode 扫到的条形码内容
     * @param scanType 扫码类型（如 EAN_13, QR_CODE 等）
     */
    fun dispatchBarcodeScan(barcode: String, scanType: String = "BARCODE") {
        currentEmitter?.sendEvent(
            mapOf(
                "barcode" to barcode,
                "scanType" to scanType,
                "timestamp" to System.currentTimeMillis(),
            )
        )
    }
}
