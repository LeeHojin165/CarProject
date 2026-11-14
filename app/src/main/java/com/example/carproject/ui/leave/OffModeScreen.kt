package com.example.carproject.ui.leave

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.OffModeChange
import com.example.carproject.data.model.TomorrowPreview
import com.example.carproject.data.repository.OffModeRepository
import com.example.carproject.data.repository.VehicleRepository
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.AppScreen
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** 11 다음 장면. 내일 출근에 배터리가 빠듯하면 충전 제안부터. */
enum class LeaveNext { Charge, Pickup }

/** design/11_OffMode.html: 퇴근 모드(Slack 상태·방해 금지)로 바꿀지 묻는다. */
class OffModeViewModel(
    private val offMode: OffModeRepository,
    private val vehicle: VehicleRepository,
    private val voice: VoiceRepository,
) : ViewModel() {
    val changes: List<OffModeChange> = offMode.changes()
    val tomorrow: TomorrowPreview = offMode.tomorrow()

    private val nextChannel = Channel<LeaveNext>(Channel.BUFFERED)
    val next: Flow<LeaveNext> = nextChannel.receiveAsFlow()

    private var spoken = false

    fun onEnter() {
        if (spoken) return
        spoken = true
        viewModelScope.launch { voice.speak("오늘도 수고했어요. 퇴근 모드로 바꿀까요?") }
    }

    /** "바꿀게요": 물은 뒤 눌렀을 때만 실행한다. */
    fun onApply() {
        voice.stop()
        viewModelScope.launch {
            offMode.apply()
            nextChannel.send(decideNext())
        }
    }

    fun onSkip() {
        voice.stop()
        nextChannel.trySend(decideNext())
    }

    fun stop() = voice.stop()

    /** 내일 왕복하고 20km도 안 남으면 충전을 먼저 권한다. */
    private fun decideNext(): LeaveNext {
        val left = vehicle.status.value.rangeKm - offMode.tomorrowRoundTripKm()
        return if (left < CHARGE_MARGIN_KM) LeaveNext.Charge else LeaveNext.Pickup
    }

    override fun onCleared() = stop()

    companion object {
        const val CHARGE_MARGIN_KM = 20

        fun factory(c: AppContainer) = viewModelFactory {
            initializer { OffModeViewModel(c.offModeRepository, c.vehicleRepository, c.voiceRepository) }
        }
    }
}

@Composable
fun OffModeScreen(viewModel: OffModeViewModel, onNext: (LeaveNext) -> Unit) {
    LaunchedEffect(viewModel) { viewModel.next.collect(onNext) }
    DisposableEffect(viewModel) {
        viewModel.onEnter()
        onDispose { viewModel.stop() }
    }
    OffModeContent(viewModel.changes, viewModel.tomorrow, viewModel::onApply, viewModel::onSkip)
}

@Composable
fun OffModeContent(
    changes: List<OffModeChange>,
    tomorrow: TomorrowPreview,
    onApply: () -> Unit,
    onSkip: () -> Unit,
) {
    val c = appColors
    AppScreen(AppIcon.Parking, "주차 중 · 퇴근 준비") {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(72.dp)) {
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
            ) {
                Text(
                    buildAnnotatedString {
                        append("오늘도 수고했어요.\n")
                        withStyle(SpanStyle(color = c.accent)) { append("퇴근 모드") }
                        append("로 바꿀까요?")
                    },
                    style = ds(88, 800, -0.03, 1.22),
                )
                Text(
                    "Slack 상태와 알림만 바꿔요. 내일 오전 9시가 되면 원래대로 돌아와요.",
                    style = ds(34, lineHeight = 1.5, color = c.textSub),
                )
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    AppButton("바꿀게요", onApply, Modifier.weight(1f), tracking = 0.0)
                    AppButton("오늘은 그대로", onSkip, Modifier.weight(1f), kind = ButtonKind.Secondary, fontSize = 40)
                }
            }
            Column(
                Modifier
                    .width(720.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(44.dp))
                    .background(c.card)
                    .padding(horizontal = 48.dp, vertical = 40.dp),
            ) {
                Text("바뀌는 것", style = ds(34, 700), modifier = Modifier.padding(bottom = 8.dp))
                changes.forEachIndexed { i, ch ->
                    if (i > 0) HorizontalDivider(thickness = 1.dp, color = c.divider)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(ch.title, style = ds(30, 700))
                            Text(ch.description, style = ds(24, color = c.textSub))
                        }
                        Text(ch.value, style = ds(30, 700, color = c.link, tabular = true), maxLines = 1)
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(c.subtle)
                        .padding(horizontal = 32.dp, vertical = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("내일 미리 보기", style = ds(26, color = c.textSub))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("첫 일정 · ${tomorrow.firstEventTitle}", style = ds(32, 700), modifier = Modifier.weight(1f).alignByBaseline())
                        Text(
                            "%02d:%02d".format(tomorrow.firstEventTime.hour, tomorrow.firstEventTime.minute),
                            style = ds(36, 800, tabular = true), modifier = Modifier.alignByBaseline(),
                        )
                    }
                    Text("바꾸면 오늘 남은 할 일 ${tomorrow.pendingTasks}개도 내일 목록으로 옮겨요.", style = ds(26, color = c.textSub))
                }
            }
        }
    }
}
