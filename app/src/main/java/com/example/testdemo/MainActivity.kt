package com.example.testdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import com.example.testdemo.auth.AuthViewModel
import com.example.testdemo.auth.LoginScreen
import com.example.testdemo.lib.navigation.AppNavHost
import com.example.testdemo.lib.navigation.FeatureGraph
import com.example.testdemo.lib.router.HomeRoute
import com.example.testdemo.ui.theme.TestDemoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var featureGraphs: Set<@JvmSuppressWildcards FeatureGraph>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestDemoTheme {
                val sessionState by (application as AppApplication).sessions.state.collectAsStateWithLifecycle()
                when {
                    !sessionState.ready -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    sessionState.session == null -> {
                        val vm: AuthViewModel = hiltViewModel()
                        LoginScreen(vm, sessionState.message)
                    }
                    else -> key(sessionState.generation) {
                        // 会话结束销毁整个业务导航作用域，取消旧 VM 请求并清空返回栈。
                        val owner = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
                        DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
                        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                            val navController = rememberNavController()
                            AppNavHost(navController, HomeRoute) { featureGraphs.forEach { it.register(this) } }
                        }
                    }
                }
            }
        }
    }
}
