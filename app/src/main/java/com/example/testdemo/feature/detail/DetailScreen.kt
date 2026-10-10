package com.example.testdemo.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.testdemo.lib.image.AppImage

/**
 * 路由版详情页：演示「带参数路由」AppRoute.Detail(id) → DetailScreen(id)。
 */
@Composable
fun DetailScreen(id: String, onBack: () -> Unit) {
    DetailContent(
        id = id,
        sourceLabel = "带参数路由 AppRoute.Detail(\"$id\")",
        onBack = onBack,
    )
}

/** 两种入口（路由 / Activity）共用同一份内容。 */
@Composable
fun DetailContent(
    id: String,
    sourceLabel: String,
    favorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("详情页", style = MaterialTheme.typography.headlineSmall)
        Text("参数 id = $id", style = MaterialTheme.typography.titleMedium)
        Text("入口：$sourceLabel", style = MaterialTheme.typography.bodySmall)

        AppImage(
            model = "https://picsum.photos/seed/$id/800/400",
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentScale = ContentScale.Crop,
        )

        if (onToggleFavorite != null) {
            OutlinedButton(onClick = onToggleFavorite) {
                Text(if (favorite) "已收藏 ✓" else "收藏")
            }
        }

        if (onBack != null) {
            Button(onClick = onBack) { Text("返回") }
        }
    }
}
