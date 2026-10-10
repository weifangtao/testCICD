package com.example.testdemo.lib.base.mvi

/**
 * 一次性事件：Snackbar、导航、弹窗等。
 *
 * 刻意与 ViewState 分开 —— 状态会重放，事件只消费一次。
 */
interface MviSingleEvent
