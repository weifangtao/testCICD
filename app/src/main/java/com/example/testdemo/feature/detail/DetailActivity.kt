package com.example.testdemo.feature.detail

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.Composable
import com.example.testdemo.lib.base.ui.AbstractMviActivity
import com.example.testdemo.lib.base.ui.TitleBarConfig
import com.example.testdemo.lib.base.ui.TitleBarTextAction

/**
 * 演示 [AbstractMviActivity]：Activity 基类自动完成
 * 「viewState → render」「singleEvent → handleSingleEvent」「viewIntents → processIntent」。
 *
 * 子类只需要：声明 [vm]、写 [render]、处理 [handleSingleEvent]。
 */
@AndroidEntryPoint
class DetailActivity :
    AbstractMviActivity<DetailIntent, DetailState, DetailEvent, DetailViewModel>() {

    /** Hilt 提供默认 factory；参数由 SavedStateHandle 从 Intent extra 读入。 */
    override val vm: DetailViewModel by viewModels()

    /** 中间标题：由状态推导，id 变了标题跟着变 */
    override fun titleBarConfig(state: DetailState) =
        TitleBarConfig(title = "详情 #${state.id}")

    /** 右侧操作区：演示「文字」按钮 */
    @Composable
    override fun TitleBarActions(state: DetailState) {
        TitleBarTextAction(if (state.favorite) "已收藏" else "收藏") {
            sendIntent(DetailIntent.ToggleFavorite)
        }
    }

    @Composable
    override fun render(state: DetailState) {
        DetailContent(
            id = state.id,
            sourceLabel = "Intent 传参 + AbstractMviActivity（标题栏由基类统一渲染）",
            onBack = null,   // 返回交给标题栏左侧按钮
        )
    }

    override fun handleSingleEvent(event: DetailEvent) {
        when (event) {
            DetailEvent.NavigateBack -> finish()
        }
    }

    companion object {
        fun intent(context: Context, id: String): Intent =
            Intent(context, DetailActivity::class.java).putExtra(ARG_DETAIL_ID, id)
    }
}
