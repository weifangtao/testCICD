package com.example.testdemo.lib.base.mvi

/**
 * 页面状态：不可变，渲染 UI 所需的全部信息。
 *
 * 纯 Kotlin 契约 —— Android 与 iOS 共用。
 * 与 `Bundle` 互转的 `MviViewStateSaver` 在 Android 侧（`lib.core.base.mvi`）。
 */
interface MviViewState
