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
    override fun estimatedMinutes(to: Place) = if (to == work) 34 else 25
}
