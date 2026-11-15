package com.example.carproject.driving

import android.content.Context
import android.content.Intent
import com.example.carproject.MainActivity
import com.example.carproject.data.model.Place
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** 앱 밖에서 생긴 일로 특정 화면을 띄워 달라는 요청. */
sealed interface ScreenRequest {
    data class ParkAsk(val place: Place) : ScreenRequest
    data object Home : ScreenRequest
    data object Login : ScreenRequest
    data object PickupWait : ScreenRequest
    data object PickupDepart : ScreenRequest
    data object TripLog : ScreenRequest
    data class Briefing(val place: Place) : ScreenRequest
}

/**
 * 기어 변경이나 시연 메뉴처럼 화면 밖에서 생긴 일을 NavHost로 전달하고, 앱을 앞으로 불러온다.
 * 백그라운드에서 화면을 여는 것은 SYSTEM_ALERT_WINDOW 권한이 있어서 허용된다.
 */
class ScreenRouter(private val context: Context) {
    private val requests = Channel<ScreenRequest>(Channel.CONFLATED)

    /** NavHost가 받아서 이동한다. 화면이 아직 없으면 생길 때까지 남아 있다. */
    val screenRequests: Flow<ScreenRequest> = requests.receiveAsFlow()

    fun open(request: ScreenRequest) {
        requests.trySend(request)
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
        )
    }
}
