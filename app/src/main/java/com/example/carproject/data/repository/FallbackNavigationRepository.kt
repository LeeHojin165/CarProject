package com.example.carproject.data.repository

import com.example.carproject.data.fake.FakeNavigationRepository
import com.example.carproject.data.model.Place
import com.example.carproject.data.pleos.PleosNavigationRepository

/** Pleos NaviHelper를 먼저 쓰고, 지원하지 않으면 가짜로 대신한다. */
class FallbackNavigationRepository(
    private val pleos: PleosNavigationRepository,
    private val fake: FakeNavigationRepository,
) : NavigationRepository {
    override suspend fun startRoute(place: Place): RouteResult =
        if (pleos.ensureReady()) pleos.startRoute(place) else fake.startRoute(place)
}
