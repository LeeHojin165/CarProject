package com.example.carproject.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carproject.data.model.ProactiveSettings
import com.example.carproject.ui.components.APP_NAME
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.components.LineIcon
import com.example.carproject.ui.components.StatusChip
import com.example.carproject.ui.home.ModalDialogFrame
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds

private val SwitchOff = Color(0xFFC6C2BD)

@Composable
fun MyPageScreen(viewModel: MyPageViewModel, onClose: () -> Unit, onDisconnected: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val proactive by viewModel.proactive.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.disconnected.collect { onDisconnected() } }
    Box(Modifier.fillMaxSize()) {
        MyPageScaffold(state, onTab = viewModel::onTab, onClose = onClose) {
            when (state.tab) {
                MyTab.Record -> RecordTab(state)
                MyTab.Account -> AccountTab(state, viewModel)
                MyTab.Commute -> CommuteTab(state, viewModel::onChangeNotAvailable)
                MyTab.Proactive -> ProactiveTab(proactive, viewModel::toggle)
                MyTab.CorpCar -> CorpCarTab(state, proactive, viewModel)
            }
        }
        state.confirm?.let { ConfirmDialog(it, state.teamChannel, viewModel::onConfirm, viewModel::cancelConfirm) }
    }
}

// ───────────────────────────── 공통 틀 ─────────────────────────────

/** design/21~25 공통: padding 56/96/72, 위쪽 바, 왼쪽 400 메뉴, 오른쪽 내용. */
@Composable
private fun MyPageScaffold(
    state: MyPageUiState,
    onTab: (MyTab) -> Unit,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = appColors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(start = 96.dp, end = 96.dp, top = 56.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(APP_NAME, style = ds(30, 700, -0.01))
                Text("마이페이지", style = ds(30, 600, color = c.textSub))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(AppIcon.Parking, "주차 중에만 열려요", fontSize = 26, iconSize = 28.dp)
                // 디자인은 72지만 버튼 최소 높이 규칙(96)을 따른다.
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(c.card)
                        .clickable(role = Role.Button, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { LineIcon(AppIcon.Close, 32.dp, color = c.text, strokeWidth = 2.4f) }
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(56.dp)) {
            Column(Modifier.width(400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(88.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                        Text(state.profile.initial, style = ds(36, 800, color = c.onAccent))
                    }
                    Column {
                        Text(state.profile.name, style = ds(34, 800, -0.01))
                        Text("${state.profile.department} · 이 차 프로필", style = ds(24, color = c.textSub))
                    }
                }
                MyTab.entries.forEach { tab ->
                    val selected = tab == state.tab
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (selected) c.card else Color.Transparent)
                            .clickable(role = Role.Tab) { onTab(tab) }
                            .padding(horizontal = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val color = if (selected) c.accent else c.textSub
                        LineIcon(tab.icon, 34.dp, color = color, strokeWidth = 2.2f)
                        Text(tab.label, style = ds(32, if (selected) 700 else 600, color = color))
                    }
                }
            }
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                content = content,
            )
        }
    }
}

/** 오른쪽 내용 제목 + 설명 + 잠깐 뜨는 안내. */
@Composable
private fun TabTitle(title: String, sub: String?, notice: String?) {
    val c = appColors
    Column {
        Text(title, style = ds(60, 800, -0.03, 1.2))
        sub?.let { Text(it, style = ds(30, lineHeight = 1.5, color = c.textSub), modifier = Modifier.padding(top = 12.dp)) }
        notice?.let { Text(it, style = ds(28, 700, color = c.accent), modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, title: String? = null, titleBottom: Dp = 8.dp, content: @Composable ColumnScope.() -> Unit) {
    val c = appColors
    Column(
        modifier
            .clip(RoundedCornerShape(40.dp))
            .background(c.card)
            .padding(horizontal = 44.dp, vertical = 36.dp),
    ) {
        title?.let { Text(it, style = ds(32, 700), modifier = Modifier.padding(bottom = titleBottom)) }
        content()
    }
}

@Composable
private fun TwoColumns(leftWeight: Float = 1f, rightWeight: Float = 1f, left: @Composable ColumnScope.() -> Unit, right: @Composable ColumnScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(Modifier.weight(leftWeight), verticalArrangement = Arrangement.spacedBy(32.dp), content = left)
        Column(Modifier.weight(rightWeight), verticalArrangement = Arrangement.spacedBy(32.dp), content = right)
    }
}

@Composable
private fun Divided(index: Int, content: @Composable () -> Unit) {
    Column {
        if (index > 0) HorizontalDivider(thickness = 1.dp, color = appColors.divider)
        content()
    }
}

/** "바꾸기" 같은 작은 버튼. 디자인은 72지만 최소 높이 규칙(96)을 따른다. */
@Composable
private fun SmallButton(text: String, onClick: () -> Unit, color: Color = appColors.text) {
    Box(
        Modifier
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(appColors.subtle)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = ds(26, 600, color = color), maxLines = 1) }
}

/** 112×64 스위치. */
@Composable
private fun Switch(on: Boolean) {
    val c = appColors
    Box(
        Modifier.size(width = 112.dp, height = 64.dp).clip(CircleShape).background(if (on) c.accent else SwitchOff),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.padding(6.dp).size(52.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White),
        )
    }
}

// ───────────────────────────── 21 나의 기록 ─────────────────────────────

@Composable
private fun ColumnScope.RecordTab(state: MyPageUiState) {
    val c = appColors
    val r = state.record
    Text(
        buildAnnotatedString {
            append("이번 달, 출퇴근길을\n")
            withStyle(SpanStyle(color = c.accent)) { append("${r.trips}번") }
            append(" 함께했어요.")
        },
        style = ds(60, 800, -0.03, 1.2),
    )
    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Card(Modifier.weight(1f).fillMaxHeight(), title = "${r.month}월 출퇴근 요약", titleBottom = 28.dp) {
            val stats = listOf(
                Triple("총 주행 거리", "${r.totalKm}" to "km", "출근 ${r.commuteToWork}번 · 퇴근 ${r.commuteToHome}번"),
                Triple("평균 출근 시간", "${r.avgCommuteMinutes}" to "분",
                    if (r.avgCommuteDeltaMinutes < 0) "지난달보다 ${-r.avgCommuteDeltaMinutes}분 빨라요" else "지난달보다 ${r.avgCommuteDeltaMinutes}분 느려요"),
                Triple("정각 전 도착", "${r.onTime}" to "번", "${r.commuteToWork}번 중"),
                Triple("대신 보낸 지각 알림", "${r.lateNoticesSent}" to "건", "모두 확인 후 보냈어요"),
            )
            stats.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(bottom = 36.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    pair.forEach { (label, value, sub) ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(label, style = ds(26, color = c.textSub))
                            Text(
                                buildAnnotatedString {
                                    append(value.first)
                                    withStyle(ds(32, 700).toSpanStyle()) { append(" ${value.second}") }
                                },
                                style = ds(64, 800, -0.03, tabular = true),
                            )
                            Text(sub, style = ds(24, color = c.textSub))
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            // 진한 남색 카드는 라이트 화면 디자인 그대로(#002C5F 위 흰 글자).
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color(0xFF002C5F))
                    .padding(horizontal = 44.dp, vertical = 36.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("퇴근길에 구한 음식", style = ds(28, 600, color = Color.White.copy(alpha = 0.8f)))
                    Text(
                        buildAnnotatedString {
                            append("${r.savedFoodKg}")
                            withStyle(ds(40, 800).toSpanStyle()) { append(" kg") }
                        },
                        style = ds(80, 800, -0.03, color = Color.White, tabular = true),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("아낀 금액", style = ds(26, color = Color.White.copy(alpha = 0.8f)))
                    Text("%,d원".format(r.savedWon), style = ds(44, 800, color = Color.White, tabular = true))
                }
            }
            Card(Modifier.fillMaxWidth().weight(1f), title = "최근 픽업") {
                state.pickups.forEachIndexed { i, p ->
                    Divided(i) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 84.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("%d.%02d".format(p.date.monthValue, p.date.dayOfMonth), style = ds(26, color = c.textSub, tabular = true), modifier = Modifier.width(120.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.store, style = ds(30, 700))
                                Text(p.detail, style = ds(24, color = c.textSub))
                            }
                            Text("${p.weightKg}kg", style = ds(30, 700, tabular = true))
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── 22 내 정보와 연결 ─────────────────────────────

@Composable
private fun ColumnScope.AccountTab(state: MyPageUiState, vm: MyPageViewModel) {
    val c = appColors
    TabTitle("내 정보와 연결", "운행기록부의 사용자 칸은 여기 적힌 이름과 부서로 채워요.", state.notice)
    TwoColumns(
        rightWeight = 1.15f,
        left = {
            Card(Modifier.fillMaxWidth(), title = "프로필") {
                listOf("이름" to state.profile.name, "부서" to state.profile.department).forEachIndexed { i, (label, value) ->
                    Divided(i) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, style = ds(28, color = c.textSub), modifier = Modifier.weight(1f))
                            Text(value, style = ds(30, 700))
                            SmallButton("바꾸기", vm::onChangeNotAvailable)
                        }
                    }
                }
                Divided(1) {
                    Column(Modifier.heightIn(min = 140.dp).padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("이 차의 프로필", style = ds(24, color = c.textSub))
                        Text("내 프로필", style = ds(34, 700))
                        Text("차 프로필마다 한 번 로그인해요", style = ds(24, color = c.textSub))
                    }
                }
            }
        },
        right = {
            Card(Modifier.fillMaxWidth(), title = "연결된 계정") {
                state.accounts.forEachIndexed { i, a ->
                    Divided(i) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(c.background), contentAlignment = Alignment.Center) {
                                Text(a.letter, style = ds(30, 800))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(a.service, style = ds(32, 700))
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(color = c.accent)) { append(if (a.connected) "연결됨" else "연결 안 됨") }
                                        withStyle(SpanStyle(color = c.textSub)) { append(" · ${a.detail}") }
                                    },
                                    style = ds(24, 700),
                                )
                            }
                            SmallButton("다시 연결", { vm.onReconnect(a) })
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(40.dp))
                    .border(2.dp, c.divider, RoundedCornerShape(40.dp))
                    .padding(horizontal = 44.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("이 차에서 연결 끊기", style = ds(30, 700))
                    Text("공용 차에서 내릴 때 쓰세요.\n다시 쓰려면 QR을 찍으면 돼요.", style = ds(24, lineHeight = 1.45, color = c.textSub))
                }
                AppButton(
                    "연결 끊기", { vm.ask(MyConfirm.Disconnect) },
                    kind = ButtonKind.Secondary, height = 96.dp, radius = 28.dp, fontSize = 30, weight = 700, horizontalPadding = 36.dp,
                )
            }
        },
    )
}

// ───────────────────────────── 23 직장과 집 ─────────────────────────────

@Composable
private fun ColumnScope.CommuteTab(state: MyPageUiState, onChange: () -> Unit) {
    val s = state.commute
    val c = appColors
    TabTitle("직장과 집", "출근 버튼을 누르면 여기 정한 대로 목적지와 도착 시각을 맞춰요.", state.notice)
    val arrive = (if (s.arriveBy.hour < 12) "오전 " else "오후 ") + "%d:%02d".format(
        if (s.arriveBy.hour % 12 == 0) 12 else s.arriveBy.hour % 12, s.arriveBy.minute,
    )
    TwoColumns(
        left = {
            Card(Modifier.fillMaxWidth(), title = "출근과 퇴근") {
                SettingRow(0, "회사", s.workName, s.workAddress, onChange)
                SettingRow(1, "출근 시각", arrive, s.workDays, onChange, valueColor = c.accent)
                SettingRow(2, "집", s.homeAddress, "퇴근길 경로와 픽업 가게 찾기에 써요", onChange)
            }
        },
        right = {
            Card(Modifier.fillMaxWidth(), title = "늦을 것 같을 때") {
                SettingRow(0, "알림 보낼 곳", "#${s.lateChannel}", "Slack · 받는 사람 ${s.lateExtraRecipients}명 더", onChange)
                SettingRow(1, "물어보는 기준", "${s.lateThresholdMinutes}분 이상 늦을 때", "내비 도착 예정 시각 기준", onChange)
                SettingRow(2, "기본 메시지", s.lateMessage, "보내기 전에 항상 읽어 드려요", onChange)
            }
        },
    )
}

@Composable
private fun SettingRow(index: Int, label: String, value: String, sub: String, onChange: () -> Unit, valueColor: Color = appColors.text) {
    val c = appColors
    Divided(index) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 140.dp).padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = ds(24, color = c.textSub))
                Text(value, style = ds(34, 700, color = valueColor, tabular = true))
                Text(sub, style = ds(24, color = c.textSub))
            }
            SmallButton("바꾸기", onChange)
        }
    }
}

// ───────────────────────────── 24 먼저 물어보기 ─────────────────────────────

@Composable
private fun ColumnScope.ProactiveTab(p: ProactiveSettings, toggle: ((ProactiveSettings) -> ProactiveSettings) -> Unit) {
    TabTitle("먼저 물어보기", "켜 둔 것만 먼저 말을 걸어요. 켜 두어도 실행하기 전에는 항상 물어봐요.", null)
    TwoColumns(
        left = {
            Card(Modifier.fillMaxWidth(), title = "출근길", titleBottom = 12.dp) {
                ToggleRow(0, "지각 알림 제안", "늦을 것 같으면 메시지를 먼저 보여줘요", p.lateAlert) { toggle { it.copy(lateAlert = !it.lateAlert) } }
                ToggleRow(1, "출발 전 차량 점검", "연료, 타이어, 경고등을 짧게 알려줘요", p.vehicleCheck) { toggle { it.copy(vehicleCheck = !it.vehicleCheck) } }
                ToggleRow(2, "멘션 메시지 요약", "나를 언급한 Slack 메시지만 읽어줘요", p.mentionSummary) { toggle { it.copy(mentionSummary = !it.mentionSummary) } }
            }
        },
        right = {
            Card(Modifier.fillMaxWidth(), title = "퇴근길과 픽업", titleBottom = 12.dp) {
                ToggleRow(0, "마감 할인 가게 제안", "선호: 빵, 반찬 · 경로에서 5분 이내", p.pickupOffer) { toggle { it.copy(pickupOffer = !it.pickupOffer) } }
                ToggleRow(1, "히터와 공조 유지", "픽업하는 동안 켜 둘지 물어봐요", p.keepClimate) { toggle { it.copy(keepClimate = !it.keepClimate) } }
                ToggleRow(2, "픽업 후 환기", "창문을 조금 열지 물어봐요", p.ventilate) { toggle { it.copy(ventilate = !it.ventilate) } }
                ToggleRow(3, "내리기 전 확인", "문, 창문, 조명, 충전 상태", p.arrivalCheck) { toggle { it.copy(arrivalCheck = !it.arrivalCheck) } }
            }
        },
    )
}

@Composable
private fun ToggleRow(index: Int, title: String, sub: String, on: Boolean, onClick: () -> Unit) {
    Divided(index) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 112.dp).clickable(role = Role.Switch, onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = ds(32, 700))
                Text(sub, style = ds(24, color = appColors.textSub))
            }
            Switch(on)
        }
    }
}

// ───────────────────────────── 25 법인 차량 ─────────────────────────────

@Composable
private fun ColumnScope.CorpCarTab(state: MyPageUiState, p: ProactiveSettings, vm: MyPageViewModel) {
    val c = appColors
    val corp = state.corp
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Box(Modifier.weight(1f)) { TabTitle("법인 차량", "켜 두면 주행이 끝날 때마다 운행기록부를 채워요.", state.notice) }
        Row(
            Modifier
                .clip(CircleShape)
                .background(c.card)
                .clickable(role = Role.Switch) { vm.toggle { it.copy(corporateCar = !it.corporateCar) } }
                .padding(start = 32.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("법인 차량", style = ds(30, 700))
            Switch(p.corporateCar)
        }
    }
    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Card(Modifier.weight(1f).fillMaxHeight()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalAlignment = Alignment.Bottom) {
                Text("${state.record.month}월 운행기록", style = ds(32, 700), modifier = Modifier.weight(1f))
                Text("국가법령정보센터 별지 서식 기준", style = ds(24, color = c.textSub))
            }
            val business = corp.monthBusinessKm
            val ratio = (corp.monthCommuteKm + business) * 1000 / corp.monthTotalKm / 10.0
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatBox("출퇴근용", "${corp.monthCommuteKm}km")
                StatBox("일반 업무용", "${business}km")
                StatBox("업무사용비율", "$ratio%", c.accent)
            }
            state.tripLogs.take(3).forEachIndexed { i, e ->
                Divided(i) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 78.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("%d.%02d".format(e.date.monthValue, e.date.dayOfMonth), style = ds(26, color = c.textSub, tabular = true), modifier = Modifier.width(90.dp))
                        Text(e.route, style = ds(28, 600), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (e.commute) "출퇴근" else e.purpose, style = ds(24, color = c.textSub), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${e.distanceKm}km", style = ds(28, 700, tabular = true), textAlign = TextAlign.End, modifier = Modifier.width(90.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "업무사용비율 = 업무용 ${corp.monthCommuteKm + business}km ÷ 총 주행 ${corp.monthTotalKm}km",
                style = ds(22, color = c.textSub),
            )
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Card(Modifier.fillMaxWidth(), title = "기본정보", titleBottom = 4.dp) {
                listOf(
                    "상호명" to corp.companyName, "사업자등록번호" to corp.businessNumber, "차종" to corp.carModel,
                    "자동차등록번호" to corp.plateNumber,
                ).forEachIndexed { i, (label, value) ->
                    Divided(i) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 80.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, style = ds(28, color = c.textSub), modifier = Modifier.weight(1f))
                            Text(value, style = ds(30, 700, tabular = true))
                        }
                    }
                }
                Divided(1) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("유류비", style = ds(28, color = c.textSub), modifier = Modifier.weight(1f))
                        Text(corp.fuelPriceSource, style = ds(30, 700))
                        SmallButton("바꾸기", vm::onChangeNotAvailable)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AppButton("엑셀로 받기", vm::onExportExcel, Modifier.weight(1f), height = 104.dp, radius = 32.dp, fontSize = 32, tracking = 0.0)
                AppButton(
                    "Slack으로 보내기", { vm.ask(MyConfirm.SendSlack) }, Modifier.weight(1f),
                    kind = ButtonKind.Secondary, height = 104.dp, radius = 32.dp, fontSize = 32,
                )
            }
        }
    }
}

@Composable
private fun RowScope.StatBox(label: String, value: String, valueColor: Color = appColors.text) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(appColors.background).padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Text(label, style = ds(24, color = appColors.textSub), maxLines = 1, softWrap = false)
        Text(value, style = ds(36, 800, color = valueColor, tabular = true), maxLines = 1, softWrap = false)
    }
}

// ───────────────────────────── 확인창 ─────────────────────────────

@Composable
private fun ConfirmDialog(confirm: MyConfirm, teamChannel: String, onYes: () -> Unit, onNo: () -> Unit) {
    val (eyebrow, title, yes) = when (confirm) {
        MyConfirm.Disconnect -> Triple("공용 차에서 내릴 때", "이 차에서 연결을 끊을까요?", "연결 끊기")
        MyConfirm.SendSlack -> Triple("운행기록 보내기", "#${teamChannel}에\n이번 달 운행기록을 보낼까요?", "보내기")
    }
    ModalDialogFrame(onNo) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(eyebrow, style = ds(30, 600, color = appColors.textSub))
            Text(title, style = ds(64, 800, -0.03, 1.2))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AppButton(yes, onYes, Modifier.weight(1f), height = 128.dp, fontSize = 42, tracking = 0.0)
            AppButton("취소", onNo, Modifier.weight(1f), kind = ButtonKind.Secondary, height = 128.dp, fontSize = 40)
        }
    }
}
