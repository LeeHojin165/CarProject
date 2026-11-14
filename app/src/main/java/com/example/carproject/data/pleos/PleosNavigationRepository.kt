package com.example.carproject.data.pleos

import android.content.Context
import android.util.Log
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteWithWaypointsRequest
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import com.example.carproject.data.model.Place
import com.example.carproject.data.repository.RouteResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Pleos Connect NaviHelper 연동.
 * NaviHelper에도 checkXXXCapabilities가 없어서 initialize 성공 여부로 지원 여부를 판단한다.
 */
class PleosNavigationRepository(context: Context) {
    private val appContext = context.applicationContext
    private val initLock = Mutex()
    private var helper: NaviHelper? = null
    private var unavailable = false

    suspend fun ensureReady(): Boolean = initLock.withLock {
        if (helper != null) return true
        if (unavailable) return false
        val ok = withContext(Dispatchers.Main) {
            runCatching {
                val instance = NaviHelper(appContext)
                val success = withTimeoutOrNull(INIT_TIMEOUT_MS) {
                    suspendCancellableCoroutine { cont ->
                        instance.initialize(
                            { if (cont.isActive) cont.resume(true) },
                            { e ->
                                Log.w(TAG, "initialize 실패", e)
                                if (cont.isActive) cont.resume(false)
                            },
                        )
                    }
                } ?: false
                if (success) helper = instance else runCatching { instance.release() }
                success
            }.getOrElse {
                Log.w(TAG, "NaviHelper를 쓸 수 없음", it)
                false
            }
        }
        if (!ok) unavailable = true
        Log.i(TAG, "Pleos NaviHelper 사용 가능: $ok")
        ok
    }

    suspend fun startRoute(place: Place, waypoints: List<Place> = emptyList()): RouteResult = withContext(Dispatchers.Main) {
        val navi = helper ?: return@withContext RouteResult.Failed("내비게이션을 쓸 수 없어요")
        val lat = place.latitude
        val lon = place.longitude
        if (lat == null || lon == null) {
            // 말로만 들은 곳은 좌표가 없으니 내비 검색 화면에 이름을 넣어 연다.
            return@withContext runCatching {
                navi.requestNavigationScreen(NavigationScreenRequest.SearchPlace(place.name))
                RouteResult.OpenedSearch
            }.getOrElse { RouteResult.Failed(it.message ?: "검색 화면을 열지 못했어요") }
        }

        val outcome = CompletableDeferred<RouteResult>()
        val listener = object : NaviHelperEventListener {
            override fun onRouteStarted(p0: RouteStartInfo) { outcome.complete(RouteResult.Started) }
            override fun onRoutingCompleted() { outcome.complete(RouteResult.Started) }
            override fun onError(p0: NaviErrorCode) {
                Log.w(TAG, "경로 오류: $p0")
                outcome.complete(RouteResult.Failed(p0.toMessage()))
            }
        }
        try {
            navi.addListener(listener)
            val destination = RouteInfo(
                latitude = lat,
                longitude = lon,
                poiName = place.name,
                poiId = "", // POI 검색 없이 좌표로만 안내
                address = place.address,
                poiSubId = "",
            )
            // 좌표가 있는 경유지만, Pleos 제한대로 최대 2곳.
            val stops = waypoints.filter { it.latitude != null && it.longitude != null }.take(2)
            if (stops.isEmpty()) {
                navi.requestRoute(destination)
            } else {
                val indexes = listOf(WaypointIndex.FIRST, WaypointIndex.SECOND)
                navi.requestRouteWithWaypoints(
                    RouteWithWaypointsRequest(
                        destination,
                        stops.mapIndexed { i, w ->
                            RequestWaypointInfo(w.latitude!!, w.longitude!!, w.name, indexes[i], "", w.address, "", "")
                        },
                    )
                )
            }
            // 경로 계산 결과를 기다린다. 응답이 없어도 요청은 들어갔으니 시작한 것으로 본다.
            withTimeoutOrNull(ROUTE_TIMEOUT_MS) { outcome.await() } ?: RouteResult.Started
        } catch (e: Exception) {
            Log.w(TAG, "requestRoute 실패", e)
            RouteResult.Failed(e.message ?: "길안내를 시작하지 못했어요")
        } finally {
            runCatching { navi.removeListener(listener) }
        }
    }

    suspend fun showNavigation() = withContext(Dispatchers.Main) {
        runCatching { helper?.requestNavigationScreen(NavigationScreenRequest.Foreground) }
            .onFailure { Log.w(TAG, "내비 화면 열기 실패", it) }
        Unit
    }

    private fun NaviErrorCode.toMessage() = when (this) {
        NaviErrorCode.CAUSE_LOW_BATTERY -> "배터리가 부족해서 경로를 찾지 못했어요"
        NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE -> "목적지가 너무 가까워요"
        NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE -> "목적지가 너무 멀어요"
        else -> "경로를 찾지 못했어요"
    }

    private companion object {
        const val TAG = "PleosNavi"
        const val INIT_TIMEOUT_MS = 5_000L
        const val ROUTE_TIMEOUT_MS = 8_000L
    }
}
