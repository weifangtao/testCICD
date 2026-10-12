package com.example.testdemo.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.testdemo.BuildConfig
import com.example.testdemo.lib.config.AppEnvironment
import com.example.testdemo.lib.config.AppEnvironmentConfigHolder

@Composable
fun LoginScreen(viewModel: AuthViewModel, sessionMessage: String?) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val environment by AppEnvironmentConfigHolder.environment.collectAsStateWithLifecycle()
    var account by rememberSaveable { mutableStateOf("") }
    // 密码只存内存，不进入 savedInstanceState。
    var password by remember(environment) { mutableStateOf("") }
    val submit = {
        if (!state.loading && account.isNotBlank() && password.isNotEmpty()) {
            viewModel.dispatch(LoginIntent.Submit(account, password))
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(32.dp))
        Text("WMS 登录", style = MaterialTheme.typography.headlineMedium)
        Text("当前环境：${environment.key}")
        OutlinedTextField(account, { account = it }, Modifier.fillMaxWidth(), label = { Text("账号") },
            singleLine = true, enabled = !state.loading, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !state.loading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }))
        (state.message ?: sessionMessage)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { submit() }, Modifier.fillMaxWidth(),
            enabled = !state.loading && account.isNotBlank() && password.isNotEmpty()) {
            if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("登录")
        }
        if (BuildConfig.DEBUG) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppEnvironment.entries.forEach { target ->
                    OutlinedButton(onClick = { viewModel.dispatch(LoginIntent.Environment(target)) },
                        enabled = !state.loading && target != environment) { Text(target.key) }
                }
            }
        }
    }
}
