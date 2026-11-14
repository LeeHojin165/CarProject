package com.example.carproject.ui.navigation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.carproject.data.model.Place

/** 장소를 화면 사이에 넘기는 인자. 모두 문자열이고 없으면 빈 값. */
object PlaceArgs {
    private const val NAME = "name"
    private const val ADDRESS = "address"
    private const val LAT = "lat"
    private const val LON = "lon"
    private val keys = listOf(NAME, ADDRESS, LAT, LON)

    /** "briefing?name={name}&..." */
    fun pattern(base: String) = "$base?" + keys.joinToString("&") { "$it={$it}" }

    fun route(base: String, place: Place): String {
        val values = listOf(place.name, place.address, place.latitude?.toString().orEmpty(), place.longitude?.toString().orEmpty())
        return "$base?" + keys.zip(values).joinToString("&") { (k, v) -> "$k=${Uri.encode(v)}" }
    }

    val navArguments = keys.map { key -> navArgument(key) { type = NavType.StringType; defaultValue = "" } }

    fun read(savedState: SavedStateHandle) = Place(
        name = savedState.get<String>(NAME).orEmpty(),
        address = savedState.get<String>(ADDRESS).orEmpty(),
        latitude = savedState.get<String>(LAT)?.toDoubleOrNull(),
        longitude = savedState.get<String>(LON)?.toDoubleOrNull(),
    )
}
