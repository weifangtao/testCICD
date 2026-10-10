package com.example.testdemo.lib.base.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.testdemo.lib.base.mvi.UiState

/**
 * 统一渲染 [UiState] 的 加载 / 空 / 错误 / 内容 四态，业务页面不必重复写。
 */
@Composable
fun <T> UiStateHost(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            UiState.Idle, UiState.Loading ->
                CircularProgressIndicator(Modifier.align(Alignment.Center))

            is UiState.Empty ->
                StateMessage(state.message, Modifier.align(Alignment.Center))

            is UiState.Error -> Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StateMessage(state.message)
                if (onRetry != null) {
                    Button(onClick = onRetry) { Text("重试") }
                }
            }

            is UiState.Success -> content(state.data)
        }
    }
}

@Composable
private fun StateMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 24.dp),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
}
