package com.example.testdemo.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.testdemo.data.remote.PostDto
import com.example.testdemo.lib.base.compose.MviScreen
import com.example.testdemo.lib.base.compose.UiStateHost
import com.example.testdemo.lib.base.mvi.UiState
import com.example.testdemo.lib.base.ui.AppTitleBar
import com.example.testdemo.lib.base.ui.TitleBarTextAction
import com.example.testdemo.lib.image.AppImage
import com.example.testdemo.lib.log.AppLogger

/**
 * 首页：主流程 demo —— MVI 全链路 + 网络 + 图片 + UiState 四态 + 统一标题栏。
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDetail: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.dispatch(HomeIntent.Load) }

    MviScreen(
        viewModel = viewModel,
        onEvent = { event ->
            when (event) {
                is HomeEvent.ShowMessage -> AppLogger.w("Home", event.message)
            }
        },
    ) { state ->
        Column(Modifier.fillMaxSize()) {
            // 统一标题栏：首页无返回，右侧放「设置」
            AppTitleBar(
                title = "统一移动端脚手架",
                onBack = null,
                actions = { TitleBarTextAction("设置") { onOpenSettings() } },
            )

            // SSOT：有本地数据就先渲染，刷新在后台进行
            val uiState: UiState<List<PostDto>> = when {
                state.posts.isNotEmpty() -> UiState.Success(state.posts)
                state.isLoading -> UiState.Loading
                state.error != null -> UiState.Error(state.error)
                state.loaded -> UiState.Empty()
                else -> UiState.Idle
            }

            // 刷新失败但库里有数据 → 提示当前展示的是缓存
            if (state.isOfflineWithCache) {
                Text(
                    text = "离线模式（刷新失败，展示本地缓存）",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            UiStateHost(
                state = uiState,
                onRetry = { viewModel.dispatch(HomeIntent.Refresh) },
            ) { posts ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(posts, key = { it.id }) { post ->
                        PostItem(post = post, onClick = { onOpenDetail(post.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PostItem(post: PostDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppImage(
                model = "https://picsum.photos/seed/${post.id}/100",
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentDescription = null,
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(post.title, style = MaterialTheme.typography.titleMedium)
                Text(post.body, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
    }
}
