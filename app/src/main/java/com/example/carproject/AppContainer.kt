package com.example.carproject

import android.content.Context
import com.example.carproject.driving.DrivingAssistant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import com.example.carproject.data.fake.FakeAuthRepository
import com.example.carproject.data.fake.FakeCalendarRepository
import com.example.carproject.data.fake.FakeMessageRepository
import com.example.carproject.data.fake.FakeNavigationRepository
import com.example.carproject.data.fake.FakeParkingRepository
import com.example.carproject.data.fake.FakeChargingRepository
import com.example.carproject.data.fake.FakeOffModeRepository
import com.example.carproject.data.fake.FakePickupRepository
import com.example.carproject.data.fake.FakeProfileRepository
import com.example.carproject.data.fake.FakeTripLogRepository
import com.example.carproject.data.fake.FakeSettingsRepository
import com.example.carproject.data.fake.FakeRecordRepository
import com.example.carproject.data.repository.SettingsRepository
import com.example.carproject.data.repository.RecordRepository
import com.example.carproject.data.repository.ProfileRepository
import com.example.carproject.data.repository.TripLogRepository
import com.example.carproject.data.repository.ChargingRepository
import com.example.carproject.data.repository.OffModeRepository
import com.example.carproject.data.repository.PickupRepository
import com.example.carproject.data.car.CarGearRepository
import com.example.carproject.data.repository.GearRepository
import com.example.carproject.data.repository.ParkingRepository
import com.example.carproject.driving.ParkingWatcher
import com.example.carproject.driving.ScreenRouter
import com.example.carproject.debug.DemoControls
import com.example.carproject.data.fake.FakePlaceRepository
import com.example.carproject.data.fake.FakeSpeechRepository
import com.example.carproject.data.fake.FakeVehicleRepository
import com.example.carproject.data.fake.FakeVoiceRepository
import com.example.carproject.data.pleos.PleosNavigationRepository
import com.example.carproject.data.pleos.PleosSpeechRepository
import com.example.carproject.data.pleos.PleosVoiceRepository
import com.example.carproject.data.repository.AuthRepository
import com.example.carproject.data.repository.CalendarRepository
import com.example.carproject.data.repository.MessageRepository
import com.example.carproject.data.repository.FallbackNavigationRepository
import com.example.carproject.data.repository.FallbackSpeechRepository
import com.example.carproject.data.repository.FallbackVoiceRepository
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.data.repository.NavigationRepository
import com.example.carproject.data.repository.PlaceRepository
import com.example.carproject.data.repository.SpeechRepository
import com.example.carproject.data.repository.VehicleRepository

/**
 * 수동 의존성 주입. 실제 API가 생기면 여기서 구현체만 바꾼다.
 */
class AppContainer(context: Context) {
    /** 앱 전체 수명의 코루틴 범위. 주행 중 판단처럼 화면과 상관없이 도는 일에 쓴다. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val fakeVehicle = FakeVehicleRepository()
    val vehicleRepository: VehicleRepository = fakeVehicle
    val authRepository: AuthRepository = FakeAuthRepository()
    val placeRepository: PlaceRepository = FakePlaceRepository()
    val calendarRepository: CalendarRepository = FakeCalendarRepository()
    val messageRepository: MessageRepository = FakeMessageRepository()
    val speechRepository: SpeechRepository = FallbackSpeechRepository(
        pleos = PleosSpeechRepository(context),
        fake = FakeSpeechRepository(),
    )
    val voiceRepository: VoiceRepository = FallbackVoiceRepository(
        pleos = PleosVoiceRepository(context),
        fake = FakeVoiceRepository(),
    )
    val navigationRepository: NavigationRepository = FallbackNavigationRepository(
        pleos = PleosNavigationRepository(context),
        fake = FakeNavigationRepository(),
    )
    val settingsRepository: SettingsRepository = FakeSettingsRepository()
    val drivingAssistant = DrivingAssistant(
        appScope, calendarRepository, messageRepository, speechRepository, voiceRepository,
        placeRepository, navigationRepository, settingsRepository,
    )
    val gearRepository: GearRepository = CarGearRepository(context)
    val parkingRepository: ParkingRepository = FakeParkingRepository()
    val offModeRepository: OffModeRepository = FakeOffModeRepository()
    val chargingRepository: ChargingRepository = FakeChargingRepository()
    val pickupRepository: PickupRepository = FakePickupRepository()
    val profileRepository: ProfileRepository = FakeProfileRepository()
    val tripLogRepository: TripLogRepository = FakeTripLogRepository()
    val recordRepository: RecordRepository = FakeRecordRepository()
    val screenRouter = ScreenRouter(context)
    val parkingWatcher = ParkingWatcher(appScope, gearRepository, drivingAssistant, placeRepository, screenRouter, settingsRepository)
    val demoControls = DemoControls(gearRepository, fakeVehicle, drivingAssistant, screenRouter, pickupRepository)
}
