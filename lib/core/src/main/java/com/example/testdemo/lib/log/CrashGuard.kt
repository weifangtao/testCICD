package com.example.testdemo.lib.log

/**
 * 全局未捕获异常兜底。
 *
 * 只做「记日志 + 交给上报」，不吞异常：处理完仍会交给上一个 handler，
 * 保证系统原本的崩溃行为（弹窗 / 退出）不被改变。
 *
 * 用法（Application.onCreate）：
 * ```
 * CrashGuard.install { throwable -> BuglyReporter.report(throwable) }
 * ```
 */
object CrashGuard {

    @Volatile
    private var installed = false

    fun install(tag: String = "Crash", onCrash: ((Throwable) -> Unit)? = null) {
        if (installed) return
        installed = true

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { AppLogger.e(tag, "uncaught exception in ${thread.name}", throwable) }
            runCatching { onCrash?.invoke(throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
