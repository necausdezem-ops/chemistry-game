package com.cris.jarvis.notify

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.cris.jarvis.ui.HoloCard
import com.cris.jarvis.ui.JarvisTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Janela flutuante (sobre outros apps) que exibe os cartões holográficos. */
class HoloOverlay(private val ctx: Context) : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var view: ComposeView? = null
    private var timer: Job? = null
    private val handler = Handler(Looper.getMainLooper())

    fun start() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        scope.launch {
            NotificationHub.current.collect { n ->
                timer?.cancel()
                if (n == null) {
                    hide()
                } else {
                    show()
                    timer = scope.launch {
                        delay(7000)
                        NotificationHub.dismissCurrent()
                    }
                }
            }
        }
    }

    fun stop() {
        timer?.cancel()
        scope.cancel()
        hide()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }

    private fun show() {
        if (view != null) return
        if (!Settings.canDrawOverlays(ctx)) return
        val v = ComposeView(ctx)
        v.setViewTreeLifecycleOwner(this)
        v.setViewTreeSavedStateRegistryOwner(this)
        v.setViewTreeViewModelStoreOwner(this)
        v.setContent {
            JarvisTheme {
                val cur by NotificationHub.current.collectAsState()
                val c = cur
                if (c != null) {
                    HoloCard(
                        n = c,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        onClick = {
                            NotificationHub.open(ctx, c)
                            NotificationHub.dismissCurrent()
                        },
                        onClose = { NotificationHub.dismissCurrent() }
                    )
                }
            }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.y = (36 * ctx.resources.displayMetrics.density).toInt()
        try {
            wm.addView(v, lp)
            view = v
        } catch (e: Exception) {
            view = null
        }
    }

    private fun hide() {
        val v = view ?: return
        view = null
        handler.post {
            try {
                wm.removeView(v)
            } catch (e: Exception) {
                // já removida
            }
        }
    }
}
