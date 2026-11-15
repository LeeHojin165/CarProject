package com.example.carproject.ui.trip

import com.example.carproject.data.AppClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.CalendarEvent
import com.example.carproject.data.model.Place
import com.example.carproject.data.repository.CalendarRepository
import com.example.carproject.data.repository.NavigationRepository
import com.example.carproject.data.repository.PlaceRepository
import com.example.carproject.data.repository.RouteResult
import com.example.carproject.data.repository.VehicleRepository
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.driving.DrivingAssistant
import com.example.carproject.ui.briefing.BriefingViewModel.Companion.koreanTime
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.AppScreen
import com.example.carproject.ui.components.LineIcon
import com.example.carproject.ui.navigation.PlaceArgs
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime

data class BusinessTripUiState(
    val place: Place,
    val event: CalendarEvent?,
    val label: String,
    val minutes: Int,
    val arrivalText: String,
    val slackText: String?,
    val tasks: List<String>,
    val done: Set<Int> = emptySet(),
    val startOdometerKm: Int,
    val reading: Boolean = false,
    val departing: Boolean = false,
    val error: String? = null,
)

/** design/19_BusinessTrip.html: 출장 출발 전 브리핑과 가서 할 일. */
class BusinessTripViewModel(
    savedState: SavedStateHandle,
    calendar: CalendarRepository,
    places: PlaceRepository,
    private val vehicle: VehicleRepository,
    private val voice: VoiceRepository,
    private val navigation: NavigationRepository,
    private val driving: DrivingAssistant,
) : ViewModel() {
    private val place = PlaceArgs.read(savedState)
    private val _uiState: MutableStateFlow<BusinessTripUiState>
    val uiState: StateFlow<BusinessTripUiState>

    private val departedChannel = Channel<Unit>(Channel.BUFFERED)
    val departed: Flow<Unit> = departedChannel.receiveAsFlow()
    private var readJob: Job? = null

    init {
        val event = calendar.todayEvents().firstOrNull { it.place == place }
        val minutes = places.estimatedMinutes(place)
        val arrival = AppClock.now().plusMinutes(minutes.toLong())
        val slack = event?.let {
            val gap = Duration.between(arrival, it.time).toMinutes()
            val what = it.title.substringAfterLast(' ')
            if (gap >= 0) "${koreanTime(it.time)} ${what}까지 |${gap}분 여유|가 있어요." else "${koreanTime(it.time)} ${what}에 |${-gap}분 늦을| 것 같아요."
        }
        _uiState = MutableStateFlow(
            BusinessTripUiState(
                place = place,
                event = event,
                label = event?.title?.substringBefore(' ') ?: place.name,
                minutes = minutes,
                arrivalText = koreanTime(arrival),
                slackText = slack,
                tasks = event?.tasks.orEmpty(),
                startOdometerKm = vehicle.status.value.odometerKm,
            )
        )
        uiState = _uiState.asStateFlow()
    }

    fun onToggleTask(index: Int) = _uiState.update {
        it.copy(done = if (index in it.done) it.done - index else it.done + index)
    }

    /** "할 일 읽어줘". 읽는 중에 다시 누르면 멈춘다. */
    fun onReadTasks() {
        if (readJob?.isActive == true) {
            readJob?.cancel(); voice.stop(); return
        }
        val tasks = _uiState.value.tasks
        val text = if (tasks.isEmpty()) "오늘 일정에 적힌 할 일이 없어요."
        else "가서 할 일은 ${tasks.size}개예요. " + tasks.mapIndexed { i, t -> "${i + 1}번, $t." }.joinToString(" ")
        readJob = viewModelScope.launch {
            _uiState.update { it.copy(reading = true) }
            try { voice.speak(text) } finally { _uiState.update { it.copy(reading = false) } }
        }
    }

    /** "출발할게요": 운행기록을 시작하고 길안내를 연다. */
    fun onDepart() {
        if (_uiState.value.departing) return
        readJob?.cancel(); voice.stop()
        val s = _uiState.value
        _uiState.update { it.copy(departing = true, error = null) }
        viewModelScope.launch {
            driving.beginBusinessTrip(place, s.event, s.startOdometerKm)
            val r = navigation.startRoute(place)
            if (r is RouteResult.Failed) {
                _uiState.update { it.copy(departing = false, error = r.reason) }
            } else {
                driving.onRouteStarted(place)
                _uiState.update { it.copy(departing = false) }
                departedChannel.send(Unit)
            }
        }
    }

    override fun onCleared() = voice.stop()

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer {
                BusinessTripViewModel(
                    createSavedStateHandle(), c.calendarRepository, c.placeRepository, c.vehicleRepository,
                    c.voiceRepository, c.navigationRepository, c.drivingAssistant,
                )
            }
        }
    }
}

@Composable
fun BusinessTripScreen(viewModel: BusinessTripViewModel, onDeparted: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.departed.collect { onDeparted() } }
    BusinessTripContent(state, viewModel::onDepart, viewModel::onReadTasks, viewModel::onToggleTask)
}

@Composable
fun BusinessTripContent(
    state: BusinessTripUiState,
    onDepart: () -> Unit,
    onReadTasks: () -> Unit,
    onToggleTask: (Int) -> Unit,
) {
    val c = appColors
    AppScreen(AppIcon.Parking, "주차 중 · 출장 준비") {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(80.dp)) {
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
            ) {
                Text(
                    buildAnnotatedString {
                        append("${state.label}까지 ${state.minutes}분.\n")
                        withStyle(SpanStyle(color = c.accent)) { append(state.arrivalText) }
                        append("에 도착해요.")
                    },
                    style = ds(92, 800, -0.03, 1.22),
                )
                Text(
                    buildAnnotatedString {
                        // "|...|" 사이는 굵게
                        state.slackText?.split('|')?.forEachIndexed { i, part ->
                            if (i % 2 == 1) withStyle(SpanStyle(color = c.text, fontWeight = FontWeight(700))) { append(part) } else append(part)
                        }
                        append(" 법인 차량이라 운행기록은 알아서 남길게요.")
                    },
                    style = ds(34, lineHeight = 1.5, color = c.textSub),
                )
                state.error?.let { Text(it, style = ds(30, 600, color = c.warning)) }
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    AppButton(if (state.departing) "경로 찾는 중…" else "출발할게요", onDepart, Modifier.weight(1f))
                    Row(
                        Modifier
                            .weight(1f)
                            .height(144.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(c.subtle)
                            .clickable(role = Role.Button, onClick = onReadTasks),
                        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LineIcon(AppIcon.Speaker, 40.dp, color = c.text, strokeWidth = 2.2f)
                        Text(if (state.reading) "읽는 중 · 눌러서 멈추기" else "할 일 읽어줘", style = ds(40, 600))
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
                Text("가서 할 일", style = ds(36, 700, -0.01))
                Row(
                    Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LineIcon(AppIcon.Pin, 28.dp, color = c.textSub, strokeWidth = 2.2f)
                    Text(
                        state.event?.let { "%d:%02d ${it.title} · ${it.location ?: state.place.address}".format(it.time.hour, it.time.minute) }
                            ?: state.place.name,
                        style = ds(28, color = c.textSub),
                    )
                }
                if (state.tasks.isEmpty()) {
                    HorizontalDivider(thickness = 1.dp, color = c.divider)
                    Text("오늘 일정에 적힌 할 일이 없어요.", style = ds(30, color = c.textSub), modifier = Modifier.padding(vertical = 26.dp))
                }
                state.tasks.forEachIndexed { i, task ->
                    val done = i in state.done
                    Column {
                        HorizontalDivider(thickness = 1.dp, color = c.divider)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Checkbox) { onToggleTask(i) }
                                .padding(vertical = 26.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (done) c.accent else Color.Transparent)
                                    .border(3.dp, if (done) c.accent else Color(0xFFC6C2BD), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) { if (done) LineIcon(AppIcon.Check, 26.dp, color = c.onAccent, strokeWidth = 3f) }
                            Text(
                                task,
                                style = ds(32, lineHeight = 1.4, color = if (done) c.textSub else c.text)
                                    .copy(textDecoration = if (done) TextDecoration.LineThrough else null),
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(c.subtle)
                        .padding(horizontal = 32.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LineIcon(AppIcon.Document, 44.dp, color = c.accent, strokeWidth = 2f)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("운행기록 시작했어요", style = ds(32, 700))
                        Text("출발 계기판 %,dkm".format(state.startOdometerKm), style = ds(28, color = c.textSub, tabular = true))
                    }
                }
            }
        }
    }
}
