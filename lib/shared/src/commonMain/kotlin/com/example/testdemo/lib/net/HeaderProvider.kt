package com.example.testdemo.lib.net

/** 业务侧提供额外请求头（渠道、版本号、门店号等）。 */
fun interface HeaderProvider {
    fun headers(): Map<String, String>
}
