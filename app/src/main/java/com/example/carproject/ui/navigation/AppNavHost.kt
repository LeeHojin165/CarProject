package com.example.carproject.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carproject.ui.arrival.ArrivalScreen
import com.example.carproject.ui.arrival.ArrivalViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import com.example.carproject.ui.leave.ChargeRouteScreen
import com.example.carproject.ui.mypage.MyPageScreen
import com.example.carproject.ui.mypage.MyPageViewModel
import com.example.carproject.ui.trip.BusinessTripScreen
import com.example.carproject.ui.trip.BusinessTripViewModel
import com.example.carproject.ui.trip.TripLogScreen
import com.example.carproject.ui.trip.TripLogViewModel
import com.example.carproject.ui.pickup.PickupDepartScreen
import com.example.carproject.ui.pickup.PickupDepartViewModel
import com.example.carproject.ui.pickup.PickupWaitScreen
import com.example.carproject.ui.pickup.PickupWaitViewModel
import com.example.carproject.ui.leave.ChargeRouteViewModel
import com.example.carproject.ui.leave.LeaveNext
import com.example.carproject.ui.leave.OffModeScreen
import com.example.carproject.ui.leave.OffModeViewModel
import com.example.carproject.ui.leave.PickupDetailScreen
import com.example.carproject.ui.leave.PickupDetailViewModel
import com.example.carproject.ui.leave.PickupOfferScreen
import com.example.carproject.ui.leave.PickupOfferViewModel
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.carproject.data.model.Place
import com.example.carproject.ui.briefing.BriefingScreen
import com.example.carproject.ui.briefing.BriefingViewModel
import com.example.carproject.driving.ScreenRequest
import com.example.carproject.ui.parking.ParkAskScreen
import com.example.carproject.ui.parking.ParkAskViewModel
import com.example.carproject.ui.parking.ParkSavedScreen
import com.example.carproject.ui.parking.ParkSavedViewModel
import com.example.carproject.AppContainer
import com.example.carproject.ui.home.HomeScreen
import com.example.carproject.ui.home.HomeViewModel
import com.example.carproject.ui.login.LoginScreen
import com.example.carproject.ui.login.LoginViewModel
import com.example.carproject.ui.theme.CarProjectTheme
import com.example.carproject.ui.theme.CommuteMode
import com.example.carproject.ui.theme.DesignCanvas

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    private const val BRIEFING_BASE = "briefing"
    private const val PARK_ASK_BASE = "park_ask"
    private const val TRIP_BASE = "business_trip"
    val TRIP = PlaceArgs.pattern(TRIP_BASE)
    const val TRIP_LOG = "trip_log"
    val BRIEFING = PlaceArgs.pattern(BRIEFING_BASE)
    val PARK_ASK = PlaceArgs.pattern(PARK_ASK_BASE)
    const val PARK_SAVED = "park_saved"
    const val ARRIVAL = "arrival"
    const val OFF_MODE = "off_mode"
    const val MY_PAGE = "my_page"
    const val PICKUP_WAIT = "pickup_wait"
    const val PICKUP_DEPART = "pickup_depart"
    const val CHARGE_ROUTE = "charge_route"
    const val PICKUP_OFFER = "pickup_offer"
    const val PICKUP_DETAIL = "pickup_detail/{${PickupDetailViewModel.ARG_STORE_ID}}"

    fun pickupDetail(storeId: String) = "pickup_detail/$storeId"

    fun briefing(place: Place) = PlaceArgs.route(BRIEFING_BASE, place)
    fun parkAsk(place: Place) = PlaceArgs.route(PARK_ASK_BASE, place)
    fun trip(place: Place) = PlaceArgs.route(TRIP_BASE, place)
}

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    // 출근길(라이트)·퇴근길(다크). 주차·도착 화면이 따른다.
    val tripMode by container.drivingAssistant.mode.collectAsStateWithLifecycle()
    // 24 "내리기 전 확인"이 꺼져 있으면 주차 뒤 바로 처음 화면으로.
    val afterParking: () -> Unit = {
        if (container.settingsRepository.proactive.value.arrivalCheck) nav.navigate(Routes.ARRIVAL)
        else nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) }
    }
    // 앱 밖에서 생긴 일(기어 P 등)로 화면을 띄워 달라는 요청.
    LaunchedEffect(nav) {
        container.screenRouter.screenRequests.collect { request ->
            when (request) {
                is ScreenRequest.ParkAsk -> nav.navigate(Routes.parkAsk(request.place)) { launchSingleTop = true }
                is ScreenRequest.Briefing -> nav.navigate(Routes.briefing(request.place))
                ScreenRequest.PickupWait -> nav.navigate(Routes.PICKUP_WAIT) { launchSingleTop = true }
                ScreenRequest.PickupDepart -> nav.navigate(Routes.PICKUP_DEPART) { launchSingleTop = true }
                ScreenRequest.TripLog -> nav.navigate(Routes.TRIP_LOG) { launchSingleTop = true }
                ScreenRequest.OffMode -> nav.navigate(Routes.OFF_MODE) {
                    popUpTo(Routes.HOME)
                    launchSingleTop = true
                }
                ScreenRequest.Login -> nav.navigate(Routes.LOGIN) {
                    popUpTo(nav.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
                ScreenRequest.Home -> nav.navigate(Routes.HOME) {
                    popUpTo(nav.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
    NavHost(navController = nav, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            Screen(CommuteMode.Morning) {
                LoginScreen(
                    viewModel = viewModel(factory = LoginViewModel.factory(container)),
                    onLater = {
                        nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                )
            }
        }
        composable(Routes.HOME) {
            val homeGear by container.gearRepository.gear.collectAsStateWithLifecycle()
            Screen(CommuteMode.Morning) {
                HomeScreen(
                    viewModel = viewModel(factory = HomeViewModel.factory(container)),
                    onDestinationChosen = { place -> nav.navigate(Routes.briefing(place)) },
                    onLeaveWork = { nav.navigate(Routes.OFF_MODE) },
                    onTripChosen = { place -> nav.navigate(Routes.trip(place)) },
                    onOpenMyPage = { nav.navigate(Routes.MY_PAGE) },
                    parked = homeGear == com.example.carproject.data.model.Gear.P,
                )
            }
        }
        composable(
            Routes.BRIEFING,
            arguments = PlaceArgs.navArguments,
        ) {
            Screen(CommuteMode.Morning) {
                BriefingScreen(
                    viewModel = viewModel(factory = BriefingViewModel.factory(container)),
                    // 길안내가 시작되면 내비 화면이 앞으로 나온다. 주행 중 팝업(05·06)은 다음 단계에서 붙인다.
                    onDeparted = { _, _ -> },
                    // 04 "출장 예약": 오늘 일정의 회사 밖 미팅 장소로 출장 브리핑(19)을 연다.
                    onBusinessTrip = {
                        container.calendarRepository.todayEvents()
                            .firstOrNull { it.outside && it.place != null }?.place
                            ?.let { nav.navigate(Routes.trip(it)) }
                    },
                )
            }
        }
        composable(Routes.PARK_ASK, arguments = PlaceArgs.navArguments) {
            Screen(tripMode) {
                ParkAskScreen(
                    viewModel = viewModel(factory = ParkAskViewModel.factory(container)),
                    onSaved = { nav.navigate(Routes.PARK_SAVED) },
                    onSkip = afterParking,
                )
            }
        }
        composable(Routes.PARK_SAVED) {
            Screen(tripMode) {
                ParkSavedScreen(
                    viewModel = viewModel(factory = ParkSavedViewModel.factory(container)),
                    onDone = afterParking,
                    onRetry = { nav.popBackStack() },
                )
            }
        }
        composable(Routes.OFF_MODE) {
            Screen(tripMode) {
                OffModeScreen(
                    viewModel = viewModel(factory = OffModeViewModel.factory(container)),
                    onNext = { next ->
                        when (next) {
                            LeaveNext.Charge -> nav.navigate(Routes.CHARGE_ROUTE)
                            LeaveNext.Pickup -> nav.navigate(Routes.PICKUP_OFFER)
                            LeaveNext.Departed -> nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) }
                        }
                    },
                )
            }
        }
        composable(Routes.CHARGE_ROUTE) {
            Screen(tripMode) {
                ChargeRouteScreen(
                    viewModel = viewModel(factory = ChargeRouteViewModel.factory(container)),
                    onNext = { departed ->
                        if (departed) nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) } else nav.navigate(Routes.PICKUP_OFFER)
                    },
                )
            }
        }
        composable(Routes.PICKUP_OFFER) {
            Screen(tripMode) {
                PickupOfferScreen(
                    viewModel = viewModel(factory = PickupOfferViewModel.factory(container)),
                    onDetail = { nav.navigate(Routes.pickupDetail(it.id)) },
                    // 길안내가 시작되면 내비가 앞으로 나온다. 앱은 처음 화면으로 돌려 둔다.
                    onDeparted = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) } },
                )
            }
        }
        composable(
            Routes.PICKUP_DETAIL,
            arguments = listOf(navArgument(PickupDetailViewModel.ARG_STORE_ID) { type = NavType.StringType }),
        ) {
            Screen(tripMode) {
                PickupDetailScreen(
                    viewModel = viewModel(factory = PickupDetailViewModel.factory(container)),
                    onBack = { nav.popBackStack() },
                    onDeparted = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) } },
                )
            }
        }
        composable(Routes.PICKUP_WAIT) {
            Screen(tripMode) {
                PickupWaitScreen(
                    viewModel = viewModel(factory = PickupWaitViewModel.factory(container)),
                    onReturned = { toDepart ->
                        if (toDepart) nav.navigate(Routes.PICKUP_DEPART) else nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) }
                    },
                )
            }
        }
        composable(Routes.PICKUP_DEPART) {
            Screen(tripMode) {
                PickupDepartScreen(
                    viewModel = viewModel(factory = PickupDepartViewModel.factory(container)),
                    // 내비가 다시 앞으로 나온다. 앱은 처음 화면으로 돌려 둔다.
                    onDone = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) } },
                )
            }
        }
        composable(Routes.TRIP, arguments = PlaceArgs.navArguments) {
            Screen(CommuteMode.Morning) {
                BusinessTripScreen(
                    viewModel = viewModel(factory = BusinessTripViewModel.factory(container)),
                    // 길안내가 시작되면 내비가 앞으로 나온다. 앱은 처음 화면으로 돌려 둔다.
                    onDeparted = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) } },
                )
            }
        }
        composable(Routes.TRIP_LOG) {
            Screen(CommuteMode.Morning) {
                TripLogScreen(
                    viewModel = viewModel(factory = TripLogViewModel.factory(container)),
                    onSaved = afterParking,
                )
            }
        }
        composable(Routes.MY_PAGE) {
            // 주행 중 잠금: 기어가 P가 아니면 마이페이지를 닫는다(홈 상태 칩이 "주차 중에만 열려요"를 보여 준다).
            val myPageGear by container.gearRepository.gear.collectAsStateWithLifecycle()
            LaunchedEffect(myPageGear) {
                if (myPageGear != com.example.carproject.data.model.Gear.P) nav.popBackStack(Routes.MY_PAGE, inclusive = true)
            }
            Screen(CommuteMode.Morning) {
                MyPageScreen(
                    viewModel = viewModel(factory = MyPageViewModel.factory(container)),
                    onClose = { nav.popBackStack() },
                    onDisconnected = { nav.navigate(Routes.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } } },
                )
            }
        }
        composable(Routes.ARRIVAL) {
            Screen(tripMode) {
                ArrivalScreen(
                    viewModel = viewModel(factory = ArrivalViewModel.factory(container)),
                    onDone = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) } },
                )
            }
        }
    }
}

/** 화면마다 라이트/다크를 정하고 1920×1080 캔버스에 맞춘다. */
@Composable
private fun Screen(mode: CommuteMode, content: @Composable () -> Unit) {
    CarProjectTheme(mode) { DesignCanvas { content() } }
}
