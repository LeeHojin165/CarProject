package com.example.carproject.data.fake

import com.example.carproject.data.model.Place
import com.example.carproject.data.model.RecentPlace
import com.example.carproject.data.repository.PlaceRepository

class FakePlaceRepository : PlaceRepository {
    private val work = Place("[회사 이름]", "[회사 주소]", 37.3947, 127.1112)
    private val home = Place("집", "[집 주소]", 37.4979, 127.0276)

    override fun workplace() = work
    override fun home() = home
    override fun recentPlaces() = listOf(
        RecentPlace(Place("[거래처 이름]", "[거래처 주소]", 37.5665, 126.9780), "지난주 목요일"),
        RecentPlace(Place("[지점 이름]", "[지점 주소]", 37.5133, 127.1001), "11월 3일"),
        RecentPlace(Place("[카페 이름]", "[카페 주소]", 37.5446, 127.0560), "10월 28일"),
    )
    override fun recentTripPlaces() = listOf(
        RecentPlace(Place("[고객사 A]", "[고객사 A 주소]", 37.5172, 127.0473), "지난주 목요일"),
        RecentPlace(Place("[협력사 B]", "[협력사 B 주소]", 37.4012, 126.9770), "11월 5일"),
        RecentPlace(Place("[지점 C]", "[지점 C 주소]", 37.3219, 127.1086), "10월 30일"),
    )

    override fun estimatedMinutes(to: Place) = when (to) {
        work -> 34
        CLIENT -> 52
        else -> 25
    }

    companion object {
        /** 오늘 일정의 고객사(수원 쪽, 시연용 좌표). */
        val CLIENT = Place("[고객사 이름]", "[고객사 주소]", 37.2636, 127.0286)
    }
}
