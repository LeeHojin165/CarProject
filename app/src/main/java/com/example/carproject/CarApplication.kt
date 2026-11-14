package com.example.carproject

import android.app.Application
import android.content.pm.ApplicationInfo
import com.example.carproject.debug.DebugMenuOverlay
import com.example.carproject.ui.overlay.OverlayPopupHost

class CarApplication : Application() {
    lateinit var container: AppContainer
        private set

    /** 디버그 빌드에서만 있다. */
    var debugMenu: DebugMenuOverlay? = null
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        OverlayPopupHost(
            context = this,
            assistant = container.drivingAssistant,
            scope = container.appScope,
            mode = { container.drivingAssistant.mode.value },
        ).start()
        container.parkingWatcher.start()
        // 시연용 숨은 메뉴는 디버그 빌드에서만. 위치는 앱 화면이 나올 때 잡는다(MainActivity).
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            debugMenu = DebugMenuOverlay(this, container.demoControls)
        }
    }
}
