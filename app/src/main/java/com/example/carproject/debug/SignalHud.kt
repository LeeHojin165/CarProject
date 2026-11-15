package com.example.carproject.debug

import android.content.Context
import android.view.Gravity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carproject.data.model.Gear
import com.example.carproject.data.model.Seat
import com.example.carproject.ui.overlay.ComposeOverlay
import com.example.carproject.ui.overlay.OverlayGeometry
import com.example.carproject.ui.theme.ds
import kotlinx.coroutines.delay

/**
 * 시연 영상용 작은 "차량 신호" 창. 앱 영역 오른쪽 위에 떠서, 앱이 실제로 받는 신호
 * (기어 GEAR_SELECTION, 배터리, 창문, 바깥 기온)를 보여 주고 값이 바뀌면 그 줄을 잠깐 강조한다.
 * 오버레이라 내비 위에서도 보인다. 디버그 빌드에서만, 시연 메뉴나 adb(--es hud on)로 켠다.
 */
class SignalHud(private val context: Context, private val demo: DemoControls) {
    private val overlay = ComposeOverlay(context)
    private val geometry = OverlayGeometry(context)

    val isShown: Boolean get() = overlay.isShown

    fun show() {
        if (overlay.isShown) return
        val area = geometry.appArea
        val screenWidth = context.resources.displayMetrics.widthPixels
        // 오른쪽 기준: 화면 오른쪽 끝에서 앱 영역 오른쪽 끝까지 + 여백
        overlay.show(gravity = Gravity.TOP or Gravity.RIGHT, x = screenWidth - area.right + 16, y = 8, touchable = false) { Hud(demo) }
    }

    fun hide() = overlay.hide()

    fun toggle() = if (overlay.isShown) hide() else show()
}

private val HudBg = Color(0xE60A0F17)
private val HudLine = Color(0xFF1C2636)
private val HudText = Color(0xFFF6F3F2)
private val HudSub = Color(0xFFA7B0BD)
private val HudAccent = Color(0xFF00AAD2)

@Composable
private fun Hud(demo: DemoControls) {
    val gear by demo.gear.collectAsStateWithLifecycle()
    val v by demo.vehicleStatus.collectAsStateWithLifecycle()
    Column(
        Modifier
            .width(440.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(HudBg)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF3DDC84)))
            Text("실시간 차량 신호", style = ds(22, 700, color = HudText))
        }
        SignalRow("기어 · GEAR_SELECTION", if (gear == Gear.P) "P 주차" else "${gear.name} 주행")
        SignalRow("배터리 · EvBattery", "${v.evBatteryPercent ?: v.fuelPercent ?: 0}% · ${v.rangeKm}km")
        SignalRow("창문 · Window", if (Seat.Passenger in v.openWindows) "조수석 열림" else "모두 닫힘")
        SignalRow("바깥 기온", "${v.outsideTempC}°C")
    }
}

/** 값이 바뀌면 2.5초 동안 배경을 강조한다. */
@Composable
private fun SignalRow(label: String, value: String) {
    var highlighted by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (first) { first = false; return@LaunchedEffect }
        highlighted = true
        delay(2_500)
        highlighted = false
    }
    val bg by animateColorAsState(if (highlighted) HudAccent.copy(alpha = 0.35f) else HudLine, tween(250), label = "hud")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = ds(20, color = HudSub), modifier = Modifier.weight(1f))
        Text(value, style = ds(24, 700, color = if (highlighted) HudText else HudText))
    }
}
