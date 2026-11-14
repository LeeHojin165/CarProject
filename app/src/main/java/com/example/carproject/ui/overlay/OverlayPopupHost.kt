package com.example.carproject.ui.overlay

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carproject.driving.DrivingAssistant
import com.example.carproject.driving.DrivingPopup
import com.example.carproject.ui.driving.DrivingPopupContent
import com.example.carproject.ui.theme.CarProjectTheme
import com.example.carproject.ui.theme.CommuteMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 주행 중 내비 위에 05·06 팝업을 띄운다. 앱 영역 위쪽 가운데에 둔다.
 * 시연 환경에서는 `adb shell appops set --user 10 com.example.carproject SYSTEM_ALERT_WINDOW allow`로 허용한다.
 */
class OverlayPopupHost(
    context: Context,
    private val assistant: DrivingAssistant,
    private val scope: CoroutineScope,
    private val mode: () -> CommuteMode,
) {
    private val overlay = ComposeOverlay(context)

    fun start() {
        scope.launch(Dispatchers.Main) {
            assistant.popup.collect { popup ->
                if (popup == DrivingPopup.None) overlay.hide() else show()
            }
        }
    }

    private fun show() {
        overlay.show {
            val popup by assistant.popup.collectAsStateWithLifecycle()
            CarProjectTheme(mode()) {
                // 그림자가 잘리지 않게 둘레에 여백을 둔다.
                Box(Modifier.padding(start = 48.dp, end = 48.dp, top = 40.dp, bottom = 56.dp)) {
                    DrivingPopupContent(popup, assistant::onSend, assistant::onDismiss)
                }
            }
        }
    }
}
