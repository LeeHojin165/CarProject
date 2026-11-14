package com.example.carproject.ui.parking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.ParkingSpot
import com.example.carproject.data.repository.ParkingRepository
import com.example.carproject.ui.components.APP_NAME
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.AppScreen
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.components.LineIcon
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds
import kotlinx.coroutines.flow.StateFlow

/** design/08_ParkSaved.html: 저장한 주차 위치와 폰으로 보낸 알림. */
class ParkSavedViewModel(parking: ParkingRepository) : ViewModel() {
    val spot: StateFlow<ParkingSpot?> = parking.latest

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { ParkSavedViewModel(c.parkingRepository) } }
    }
}

@Composable
fun ParkSavedScreen(viewModel: ParkSavedViewModel, onDone: () -> Unit, onRetry: () -> Unit) {
    val spot by viewModel.spot.collectAsStateWithLifecycle()
    spot?.let { ParkSavedContent(it, onDone, onRetry) }
}

@Composable
fun ParkSavedContent(spot: ParkingSpot, onDone: () -> Unit, onRetry: () -> Unit) {
    val c = appColors
    val time = "%02d:%02d".format(spot.parkedAt.hour, spot.parkedAt.minute)
    AppScreen(AppIcon.Parking, "주차 중 · 위치 저장") {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(72.dp)) {
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = c.accent)) { append(spot.label) }
                        append("에\n세웠다고 기억할게요.")
                    },
                    style = ds(84, 800, -0.03, 1.22),
                )
                Text("폰으로도 보냈어요. 나중에 차를 찾을 때 열어 보세요.", style = ds(34, lineHeight = 1.5, color = c.textSub))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    AppButton("좋아요", onDone, Modifier.weight(1f))
                    AppButton("다시 말하기", onRetry, Modifier.weight(1f), kind = ButtonKind.Secondary, fontSize = 40)
                }
            }
            Column(
                Modifier
                    .width(680.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(44.dp))
                    .background(c.card)
                    .padding(horizontal = 48.dp, vertical = 44.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Text("폰에 도착한 알림", style = ds(28, 600, color = c.textSub))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(c.background)
                        .padding(horizontal = 36.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Box(
                        Modifier.size(88.dp).clip(RoundedCornerShape(26.dp)).background(c.accent),
                        contentAlignment = Alignment.Center,
                    ) { LineIcon(AppIcon.PinBold, 56.dp, color = c.onAccent, strokeWidth = 2.2f) }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("$APP_NAME · 지금", style = ds(26, color = c.textSub))
                        Text("내 차는 ${spot.label}", style = ds(36, 800))
                        Text("${spot.placeName} 주차장 · $time", style = ds(28, lineHeight = 1.45, color = c.textSub))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "출근길이든 퇴근길이든, 기어를 P로 바꾸면 항상 물어봐요.",
                    style = ds(28, lineHeight = 1.55, color = c.textSub),
                )
            }
        }
    }
}
