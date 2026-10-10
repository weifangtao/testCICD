package com.example.testdemo.lib.base.mvi

import android.os.Bundle

/**
 * [MviViewState] 与 [Bundle] 的双向转换，用于进程被杀后恢复页面状态。
 *
 * Android 专属（依赖 `Bundle`），所以放在 core 而不是 shared。
 */
interface MviViewStateSaver<S : MviViewState> {
    fun S.toBundle(): Bundle
    fun restore(bundle: Bundle?): S
}
