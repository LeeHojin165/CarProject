package com.example.carproject.debug

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carproject.data.model.Gear
import com.example.carproject.data.model.Seat
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.LineIcon
import com.example.carproject.ui.overlay.ComposeOverlay
import com.example.carproject.ui.overlay.OverlayGeometry
import com.example.carproject.ui.theme.CarProjectTheme
import com.example.carproject.ui.theme.CommuteMode
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds

/**
 * 시연용 숨은 디버그 메뉴. 앱 영역 왼쪽 위 구석의 투명한 칸을 길게 누르면 패널이 열린다.
 * 오버레이라서 우리 앱 화면에서도, 내비가 앞에 있을 때도 열 수 있다. 디버그 빌드에서만 켠다.
 */
class DebugMenuOverlay(context: Context, private val demo: DemoControls) {
    private val hotspot = ComposeOverlay(context)
    private val panel = ComposeOverlay(context)
    private val geometry = OverlayGeometry(context)
    private var placedAt: Rect? = null
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 앱 화면이 나올 때마다 부른다. 앱이 막 시작된 때는 앱 영역을 몰라(화면 전체가 잡힘)
     * 엉뚱한 곳에 놓이므로, 앱 영역이 바뀌었으면 다시 놓는다.
     */
    fun ensureShown(retries: Int = 10) {
        if (!geometry.isAppAreaSettled) {
            // 창이 제자리를 잡을 때까지 잠시 뒤 다시 본다.
            if (retries > 0) handler.postDelayed({ ensureShown(retries - 1) }, 500)
            return
        }
        val area = geometry.appArea
        if (hotspot.isShown && area == placedAt) return
        hotspot.hide()
        placedAt = area
        hotspot.show(gravity = Gravity.TOP or Gravity.LEFT, x = area.left) {
            // 눈에 보이지 않는 칸. 길게 누르면 메뉴를 연다.
            Box(
                Modifier
                    .size(HOTSPOT_SIZE.dp)
                    .pointerInput(Unit) { detectTapGestures(onLongPress = { togglePanel() }) },
            )
        }
    }

    private fun togglePanel() {
        if (panel.isShown) panel.hide() else panel.show(y = 24) {
            CarProjectTheme(CommuteMode.Evening) { DebugPanel(demo, onClose = panel::hide) }
        }
    }

    private companion object {
        const val HOTSPOT_SIZE = 96
    }
}

@Composable
private fun DebugPanel(demo: DemoControls, onClose: () -> Unit) {
    val c = appColors
    val gear by demo.gear.collectAsStateWithLifecycle()
    val mode by demo.mode.collectAsStateWithLifecycle()
    val v by demo.vehicleStatus.collectAsStateWithLifecycle()
    val batteryLow = (v.evBatteryPercent ?: 100) < 20
    val windowOpen = Seat.Passenger in v.openWindows

    Column(
        Modifier
            .padding(40.dp)
            .width(1120.dp)
            .shadow(40.dp, RoundedCornerShape(40.dp))
            .clip(RoundedCornerShape(40.dp))
            .background(c.card)
            .padding(horizontal = 44.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("시연 메뉴", style = ds(40, 800), modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(c.subtle)
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { LineIcon(AppIcon.Close, 36.dp, color = c.text, strokeWidth = 2.4f) }
        }
        Section("모드") {
            Choice("출근 · 라이트", mode == CommuteMode.Morning) { demo.setMode(CommuteMode.Morning) }
            Choice("퇴근 · 다크", mode == CommuteMode.Evening) { demo.setMode(CommuteMode.Evening) }
        }
        Section("기어") {
            // P로 바꾸면 주차 화면으로 넘어가니 메뉴를 닫는다(촬영 화면에 남지 않게).
            Choice("P 주차", gear == Gear.P) { demo.setGear(Gear.P); onClose() }
            Choice("D 주행", gear == Gear.D) { demo.setGear(Gear.D) }
        }
        Section("배터리") {
            Choice("62% 보통", !batteryLow) { demo.setBatteryLow(false) }
            Choice("14% 부족", batteryLow) { demo.setBatteryLow(true) }
        }
        Section("조수석 창문") {
            Choice("닫힘", !windowOpen) { demo.setPassengerWindowOpen(false) }
            Choice("열림", windowOpen) { demo.setPassengerWindowOpen(true) }
        }
        Section("워셔액") {
            Choice("충분", !v.washerFluidLow) { demo.setWasherLow(false) }
            Choice("부족", v.washerFluidLow) { demo.setWasherLow(true) }
        }
        Section("장면") {
            Action("지각 알림") { demo.showLateNotice(); onClose() }
            Action("픽업 알림") { demo.showPickupApproach(); onClose() }
            Action("팝업 닫기") { demo.closePopups() }
            Action("처음 화면") { demo.goHome(); onClose() }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = ds(28, 600, color = appColors.textSub), modifier = Modifier.width(200.dp))
        content()
    }
}

/** 지금 상태를 보여주는 선택 버튼. 고른 쪽이 강조색. */
@Composable
private fun RowScope.Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = appColors
    DebugButton(label, if (selected) c.accent else c.subtle, if (selected) c.onAccent else c.text, onClick)
}

@Composable
private fun RowScope.Action(label: String, onClick: () -> Unit) {
    val c = appColors
    DebugButton(label, c.subtle, c.text, onClick)
}

@Composable
private fun RowScope.DebugButton(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .height(96.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = ds(32, 700, color = fg)) }
}
