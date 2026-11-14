package com.example.carproject.data.fake

import android.util.Log
import com.example.carproject.data.model.Place
import com.example.carproject.data.repository.RouteResult
import kotlinx.coroutines.delay

/** 내비를 쓸 수 없을 때. 안내를 시작한 것처럼만 한다. */
class FakeNavigationRepository {
    suspend fun startRoute(place: Place): RouteResult {
        Log.i("FakeNavi", "가짜 길안내 시작: ${place.name}")
        delay(500)
        return RouteResult.Simulated
    }
}
