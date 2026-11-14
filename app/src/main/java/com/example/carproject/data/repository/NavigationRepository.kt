package com.example.carproject.data.repository

import com.example.carproject.data.model.Place

sealed interface RouteResult {
    /** 길안내가 시작됐다. 내비 화면이 앞으로 나온다. */
    data object Started : RouteResult

    /** 좌표가 없어 내비 검색 화면을 열었다. 사용자가 고르면 안내가 시작된다. */
    data object OpenedSearch : RouteResult

    /** 내비를 쓸 수 없어 가짜로 대신했다. */
    data object Simulated : RouteResult

    data class Failed(val reason: String) : RouteResult
}

interface NavigationRepository {
    /** 목적지로 길안내를 시작한다. waypoints는 최대 2곳(Pleos 제한), 들르는 순서대로. */
    suspend fun startRoute(place: Place, waypoints: List<Place> = emptyList()): RouteResult

    /** 이미 안내 중인 내비 화면을 앞으로 불러온다. */
    suspend fun showNavigation()
}
