package com.example.carproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.carproject.ui.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CarApplication).container
        setContent { AppNavHost(container) }
    }

    override fun onResume() {
        super.onResume()
        // 시연 메뉴의 숨은 칸을 앱 영역 구석에 놓는다(DEMO_TOOLS일 때만 있음).
        (application as CarApplication).debugMenu?.ensureShown()
    }
}
