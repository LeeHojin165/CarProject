package com.example.carproject.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.CommuteSettings
import com.example.carproject.data.model.CorporateCarInfo
import com.example.carproject.data.model.LinkedAccount
import com.example.carproject.data.model.MonthlyRecord
import com.example.carproject.data.model.PickupHistory
import com.example.carproject.data.model.ProactiveSettings
import com.example.carproject.data.model.Profile
import com.example.carproject.data.model.TripLogEntry
import com.example.carproject.data.repository.MessageRepository
import com.example.carproject.data.repository.ProfileRepository
import com.example.carproject.data.repository.RecordRepository
import com.example.carproject.data.repository.SettingsRepository
import com.example.carproject.data.repository.TripLogRepository
import com.example.carproject.driving.DrivingAssistant
import com.example.carproject.ui.components.AppIcon
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** design/21~25 왼쪽 메뉴. */
enum class MyTab(val label: String, val icon: AppIcon) {
    Record("나의 기록", AppIcon.Chart),
    Account("내 정보와 연결", AppIcon.PersonWide),
    Commute("직장과 집", AppIcon.Building),
    Proactive("먼저 물어보기", AppIcon.Ask),
    CorpCar("법인 차량", AppIcon.Ledger),
}

/** 실행 전에 묻는 확인창(CLAUDE.md). */
enum class MyConfirm { Disconnect, SendSlack }

data class MyPageUiState(
    val tab: MyTab = MyTab.Record,
    val profile: Profile,
    val record: MonthlyRecord,
    val pickups: List<PickupHistory>,
    val accounts: List<LinkedAccount>,
    val commute: CommuteSettings,
    val corp: CorporateCarInfo,
    val tripLogs: List<TripLogEntry>,
    val teamChannel: String,
    /** 잠깐 보여주는 안내 문구 */
    val notice: String? = null,
    val confirm: MyConfirm? = null,
)

class MyPageViewModel(
    private val profiles: ProfileRepository,
    private val records: RecordRepository,
    private val settings: SettingsRepository,
    private val tripLog: TripLogRepository,
    messages: MessageRepository,
    driving: DrivingAssistant,
) : ViewModel() {
    private val _uiState: MutableStateFlow<MyPageUiState>
    val uiState: StateFlow<MyPageUiState>
    val proactive: StateFlow<ProactiveSettings> = settings.proactive

    private val disconnectedChannel = Channel<Unit>(Channel.BUFFERED)
    val disconnected: Flow<Unit> = disconnectedChannel.receiveAsFlow()
    private var noticeJob: Job? = null

    init {
        // 오늘 구한 음식은 이번 달 기록에 더한다.
        val base = records.thisMonth()
        val todayKg = driving.savedFoodKgToday
        val record = base.copy(
            savedFoodKg = ((base.savedFoodKg + todayKg) * 10).roundToInt() / 10.0,
        )
        _uiState = MutableStateFlow(
            MyPageUiState(
                profile = profiles.profile(),
                record = record,
                pickups = records.recentPickups(),
                accounts = profiles.linkedAccounts(),
                commute = settings.commute(),
                corp = records.corporateCar(),
                tripLogs = tripLog.entries(),
                teamChannel = messages.teamChannel(),
            )
        )
        uiState = _uiState.asStateFlow()
    }

    fun onTab(tab: MyTab) = _uiState.update { it.copy(tab = tab, notice = null) }

    fun toggle(transform: (ProactiveSettings) -> ProactiveSettings) = settings.update(transform)

    /** 시연판에서 바꿀 수 없는 항목의 "바꾸기". */
    fun onChangeNotAvailable() = notice("시연판에서는 바꿀 수 없어요")

    fun onReconnect(account: LinkedAccount) = notice("${account.service} 연결을 새로 고쳤어요")

    fun onExportExcel() {
        viewModelScope.launch {
            tripLog.exportExcel()
            notice("운행기록 엑셀 파일을 폰으로 보냈어요")
        }
    }

    fun ask(confirm: MyConfirm) = _uiState.update { it.copy(confirm = confirm) }
    fun cancelConfirm() = _uiState.update { it.copy(confirm = null) }

    /** 확인창에서 "네"를 눌렀을 때만 실행한다. */
    fun onConfirm() {
        val c = _uiState.value.confirm ?: return
        _uiState.update { it.copy(confirm = null) }
        viewModelScope.launch {
            when (c) {
                MyConfirm.Disconnect -> {
                    profiles.disconnect()
                    disconnectedChannel.send(Unit)
                }
                MyConfirm.SendSlack -> {
                    tripLog.sendToSlack(_uiState.value.teamChannel)
                    notice("#${_uiState.value.teamChannel}에 운행기록을 보냈어요")
                }
            }
        }
    }

    private fun notice(text: String) {
        noticeJob?.cancel()
        _uiState.update { it.copy(notice = text) }
        noticeJob = viewModelScope.launch {
            delay(3_000)
            _uiState.update { it.copy(notice = null) }
        }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer {
                MyPageViewModel(
                    c.profileRepository, c.recordRepository, c.settingsRepository,
                    c.tripLogRepository, c.messageRepository, c.drivingAssistant,
                )
            }
        }
    }
}
