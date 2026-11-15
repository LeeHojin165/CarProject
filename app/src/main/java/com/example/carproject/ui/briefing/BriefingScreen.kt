package com.example.carproject.ui.briefing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carproject.data.model.Mention
import com.example.carproject.data.model.Place
import com.example.carproject.data.repository.RouteResult
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.AppScreen
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.components.CardTitle
import com.example.carproject.ui.components.LineIcon
import com.example.carproject.ui.theme.CarProjectTheme
import com.example.carproject.ui.theme.DesignCanvas
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds

@Composable
fun BriefingScreen(
    viewModel: BriefingViewModel,
    onDeparted: (Place, RouteResult) -> Unit,
    onBusinessTrip: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.departed.collect { onDeparted(state.place, it) } }
    BriefingContent(state, viewModel::onDepart, viewModel::onReadMentions, onBusinessTrip)
}

@Composable
fun BriefingContent(
    state: BriefingUiState,
    onDepart: () -> Unit,
    onReadMentions: () -> Unit,
    onBusinessTrip: () -> Unit,
) {
    val c = appColors
    AppScreen(AppIcon.Parking, "주차 중 · 출근 준비") {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(80.dp)) {
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
            ) {
                Text(
                    buildAnnotatedString {
                        append("${state.placeLabel}까지 ${state.travelMinutes}분.\n")
                        withStyle(SpanStyle(color = c.accent)) { append(state.arrivalText) }
                        append("에 도착해요.")
                    },
                    style = ds(92, 800, -0.03, 1.22),
                )
                Text(
                    buildAnnotatedString {
                        val strong = SpanStyle(color = c.text, fontWeight = FontWeight(700))
                        state.nextEvent?.let { gap ->
                            append("${gap.label}까지 ")
                            if (gap.minutes >= 0) {
                                withStyle(strong) { append("${gap.minutes}분 여유") }
                                append("가 있어요. ")
                            } else {
                                withStyle(strong.copy(color = c.warning)) { append("${-gap.minutes}분 늦을") }
                                append(" 것 같아요. ")
                            }
                        }
                        append(state.destinationSentence)
                    },
                    style = ds(34, lineHeight = 1.5, color = c.textSub),
                )
                (state.departure as? DepartureState.Failed)?.let {
                    Text(it.reason, style = ds(30, 600, color = c.warning))
                }
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    AppButton(
                        when (state.departure) {
                            DepartureState.Requesting -> "경로 찾는 중…"
                            is DepartureState.Failed -> "다시 출발하기"
                            DepartureState.Idle -> "출발할게요"
                        },
                        onDepart, Modifier.weight(1f),
                    )
                    // 아이콘 + 글자 버튼. 읽을 멘션이 없으면 감춘다.
                    if (state.mentions.isNotEmpty()) Row(
                        Modifier
                            .weight(1f)
                            .height(144.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(c.subtle)
                            .clickable(role = Role.Button, onClick = onReadMentions),
                        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LineIcon(AppIcon.Speaker, 40.dp, color = c.text, strokeWidth = 2.2f)
                        Text(if (state.readingMentions) "읽는 중 · 눌러서 멈추기" else "멘션 메시지 읽어줘", style = ds(40, 600))
                    }
                }
            }
            Column(
                Modifier
                    .width(660.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(44.dp))
                    .background(c.card)
                    .padding(horizontal = 48.dp, vertical = 44.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CardTitle("오늘 챙길 것")
                state.events.forEachIndexed { i, e -> EventRow(e, showDivider = i > 0, onBusinessTrip) }
                Spacer(Modifier.weight(1f))
                if (state.mentions.isNotEmpty()) MentionBox(state.mentions)
            }
        }
    }
}

@Composable
private fun EventRow(e: EventItem, showDivider: Boolean, onBusinessTrip: () -> Unit) {
    val c = appColors
    Column {
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = c.divider)
        Row(
            Modifier.fillMaxWidth().padding(vertical = if (e.location != null) 22.dp else 26.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                e.timeText, style = ds(34, 700, tabular = true),
                modifier = Modifier.width(120.dp).align(if (e.location != null) Alignment.Top else Alignment.CenterVertically)
                    .padding(top = if (e.location != null) 4.dp else 0.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(e.title, style = ds(32, lineHeight = 1.4))
                e.location?.let {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LineIcon(AppIcon.Pin, 26.dp, color = c.textSub, strokeWidth = 2.2f)
                        Text(it, style = ds(26, lineHeight = 1.4, color = c.textSub))
                    }
                }
            }
            if (e.outside) {
                // 디자인은 높이 76이지만 버튼 최소 높이 규칙(96)을 따른다.
                Row(
                    Modifier
                        .height(96.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.subtle)
                        .clickable(role = Role.Button, onClick = onBusinessTrip)
                        .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LineIcon(AppIcon.Briefcase, 28.dp, color = c.text, strokeWidth = 2.2f)
                    Text("출장 예약", style = ds(28, 700))
                }
            }
        }
    }
}

@Composable
private fun MentionBox(mentions: List<Mention>) {
    val c = appColors
    val first = mentions.first()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(c.subtle)
            .padding(horizontal = 32.dp, vertical = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIcon(AppIcon.Chat, 44.dp, color = c.accent, strokeWidth = 2f)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("나를 언급한 메시지 ${mentions.size}개", style = ds(32, 700))
            Text("${first.sender} · #${first.channel}", style = ds(28, color = c.textSub))
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun BriefingPreview() = CarProjectTheme {
    DesignCanvas {
        BriefingContent(
            BriefingUiState(
                place = Place("[회사 이름]", "[회사 주소]"),
                placeLabel = "회사", travelMinutes = 34, arrivalText = "8시 46분",
                nextEvent = NextEventGap("9시 주간 회의", 14),
                destinationSentence = "목적지는 회사로 맞춰 뒀어요.",
                events = listOf(
                    EventItem("9:00", "주간 회의", null, false),
                    EventItem("11:30", "고객사 미팅", "[미팅 장소]", true),
                    EventItem("15:00", "디자인 리뷰", null, false),
                ),
                mentions = listOf(Mention("[보낸 사람]", "[채널]", "")),
            ),
            {}, {}, {},
        )
    }
}
