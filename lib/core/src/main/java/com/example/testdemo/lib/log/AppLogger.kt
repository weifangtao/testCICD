package com.example.testdemo.lib.log

import android.util.Log

interface Logger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

object AppLogger {
    private val androidLogger = object : Logger {
        override fun debug(tag: String, message: String) { Log.d(tag, message) }
        override fun info(tag: String, message: String) { Log.i(tag, message) }
        override fun warn(tag: String, message: String, throwable: Throwable?) { Log.w(tag, message, throwable) }
        override fun error(tag: String, message: String, throwable: Throwable?) { Log.e(tag, message, throwable) }
    }
    @Volatile private var delegate: Logger = androidLogger
    @Volatile var enabled: Boolean = true

    fun install(logger: Logger, enabled: Boolean = true) { delegate = logger; this.enabled = enabled }
    fun d(tag: String, message: String) { if (enabled) delegate.debug(tag, message) }
    fun i(tag: String, message: String) { if (enabled) delegate.info(tag, message) }
    fun w(tag: String, message: String, throwable: Throwable? = null) { if (enabled) delegate.warn(tag, message, throwable) }
    fun e(tag: String, message: String, throwable: Throwable? = null) { if (enabled) delegate.error(tag, message, throwable) }
}
