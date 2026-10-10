package com.example.testdemo.lib.net

/**
 * HTTP 日志脱敏。
 *
 * 日志对排查问题很有价值，但请求头、URL 参数和响应体里经常带凭证。
 * 这里在**写日志之前**把敏感值替换成 `***`，
 * 所以即使开着 HTTP 日志，也不会把 token 打进 Logcat 或崩溃平台。
 *
 * 覆盖四类：
 * 1. 敏感请求头：`Authorization` / `Cookie` / `X-Api-Key` / `*token*` / `*secret*`
 * 2. URL 查询参数：`?token=xxx&access_token=yyy`
 * 3. URL 内嵌 Basic Auth：`https://user:pass@host`
 * 4. 响应体里的敏感字段：`{"access_token":"xxx"}`
 */
internal fun String.redactSensitive(): String {
    var result = this

    // ① 敏感请求头（Ktor 日志形如 `-> Authorization: Bearer xxx`）
    result = SENSITIVE_HEADER_LINE.replace(result) { match ->
        "${match.groupValues[1]}${match.groupValues[2]}${match.groupValues[3]}***"
    }

    // ② URL 查询参数
    result = SENSITIVE_QUERY_PARAM.replace(result) { match ->
        "${match.groupValues[1]}***"
    }

    // ③ URL 内嵌 Basic Auth
    result = BASIC_AUTH_IN_URL.replace(result) { match ->
        "${match.groupValues[1]}://***:***@"
    }

    // ④ 响应体里的敏感字段（登录接口常见）
    result = SENSITIVE_JSON_FIELD.replace(result) { match ->
        "${match.groupValues[1]}\"***\""
    }

    return result
}

/** `-> Authorization: Bearer xxx` → `-> Authorization: ***` */
private val SENSITIVE_HEADER_LINE = Regex(
    pattern = """^(\s*(?:->|<-)?\s*)""" +
        """([A-Za-z0-9_-]*(?:authorization|cookie|api[-_]?key|token|secret|password)[A-Za-z0-9_-]*)""" +
        """(\s*:\s*)(.+)$""",
    options = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
)

/** `?token=abc&access_token=def` → `?token=***&access_token=***` */
private val SENSITIVE_QUERY_PARAM = Regex(
    pattern = """([?&](?:access_token|refresh_token|id_token|token|api_key|apikey|password|passwd|sign|signature|secret)=)[^&\s]+""",
    options = setOf(RegexOption.IGNORE_CASE),
)

/** URL 内嵌的 Basic Auth 凭证会被替换掉（user:pass → 掩码） */
private val BASIC_AUTH_IN_URL = Regex(
    pattern = """(https?)://[^/\s@]+@""",
    options = setOf(RegexOption.IGNORE_CASE),
)

/** `{"access_token":"xxx"}` → `{"access_token":"***"}` */
private val SENSITIVE_JSON_FIELD = Regex(
    pattern = "(\"(?:access_token|refresh_token|id_token|token|password|secret|api[-_]?key)\"\\s*:\\s*)\"[^\"]*\"",
    options = setOf(RegexOption.IGNORE_CASE),
)
