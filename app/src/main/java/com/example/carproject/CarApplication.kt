package com.example.carproject

import android.app.Application
import com.example.carproject.debug.DebugMenuOverlay
import com.example.carproject.ui.overlay.OverlayPopupHost

class CarApplication : Application() {
    lateinit var container: AppContainer
        private set

    /** 시연 도구(BuildConfig.DEMO_TOOLS)가 켜져 있을 때만 있다. */
    var debugMenu: DebugMenuOverlay? = null
        private set

    /** 시연용 "차량 신호" 창. 시연 도구(BuildConfig.DEMO_TOOLS)가 켜져 있을 때만 있다. */
    var signalHud: com.example.carproject.debug.SignalHud? = null
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
        // 시연용 숨은 메뉴는 DEMO_TOOLS가 켜져 있을 때만. 위치는 앱 화면이 나올 때 잡는다(MainActivity).
        if (BuildConfig.DEMO_TOOLS) {
            signalHud = com.example.carproject.debug.SignalHud(this, container.demoControls)
            debugMenu = DebugMenuOverlay(this, container.demoControls) { signalHud?.toggle() }
        }
    }
}
