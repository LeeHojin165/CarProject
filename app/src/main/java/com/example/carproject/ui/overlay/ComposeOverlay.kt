package com.example.carproject.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.carproject.ui.theme.DESIGN_WIDTH

/**
 * Pleos 화면에서 오버레이 창 위치를 잡기 위한 값.
 * currentWindowMetrics는 앱 영역(왼쪽 계기판 패널을 뺀 오른쪽), maximumWindowMetrics는 화면 전체를 준다.
 * 오버레이의 x는 화면 전체 기준이고, y=0은 상태 표시줄 바로 아래다.
 */
class OverlayGeometry(context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val resources = context.resources

    val appArea: Rect
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) windowManager.currentWindowMetrics.bounds
        else fallback()

    /**
     * 앱 화면이 막 열린 직후에는 Pleos가 창을 오른쪽 패널로 옮기기 전이라 앱 영역이 (0,0)에 크기만 잡혀 있다.
     * 1~3초 뒤 제자리(예: 853,90)로 바뀌므로, 그 전에는 위치를 믿지 않는다.
     */
    val isAppAreaSettled: Boolean
        get() = appArea.let { !(it.left == 0 && it.top == 0 && it.width() < screen.width()) }

    val screen: Rect
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) windowManager.maximumWindowMetrics.bounds
        else fallback()

    /** 디자인 1920 기준 → px. 앱 화면(DesignCanvas)과 같은 크기로 보이게 한다. */
    val scale: Float get() = appArea.width() / DESIGN_WIDTH.toFloat()

    /** CENTER_HORIZONTAL 창을 앱 영역 가운데에 두는 x. */
    val centerOffsetX: Int get() = appArea.centerX() - screen.centerX()

    private fun fallback() = resources.displayMetrics.let { Rect(0, 0, it.widthPixels, it.heightPixels) }
}

/**
 * Compose로 그리는 오버레이 창 하나. SYSTEM_ALERT_WINDOW 권한이 필요하다.
 * 안쪽에서는 디자인 px 값을 그대로 dp로 쓰면 된다.
 */
class ComposeOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val geometry = OverlayGeometry(context)
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null

    val isShown: Boolean get() = view != null

    /**
     * @param gravity 창 기준점. CENTER_HORIZONTAL이면 앱 영역 가운데에 맞춘다.
     * @param x gravity가 LEFT일 때 화면 왼쪽에서의 거리(px). null이면 앱 영역 왼쪽.
     */
    fun show(
        gravity: Int = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
        x: Int? = null,
        y: Int = 0,
        content: @Composable () -> Unit,
    ): Boolean {
        if (view != null) return true
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "다른 앱 위에 그리기 권한이 없어 오버레이를 띄우지 못함")
            return false
        }
        val scale = geometry.scale
        val lifecycleOwner = OverlayOwner().also { it.start() }
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(scale, 1f), content = content)
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            this.gravity = gravity
            this.x = when {
                gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL -> geometry.centerOffsetX
                else -> x ?: geometry.appArea.left
            }
            this.y = y
        }
        return runCatching { windowManager.addView(composeView, params) }
            .onSuccess { view = composeView; owner = lifecycleOwner }
            .onFailure { Log.w(TAG, "오버레이 추가 실패", it); lifecycleOwner.stop() }
            .isSuccess
    }

    fun hide() {
        view?.let { runCatching { windowManager.removeView(it) } }
        owner?.stop()
        view = null
        owner = null
    }

    /** Activity 밖 ComposeView에 필요한 수명주기. */
    private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val savedState = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

        fun start() {
            savedState.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    private companion object {
        const val TAG = "ComposeOverlay"
    }
}
