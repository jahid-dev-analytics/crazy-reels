package com.flyingman.crazyreels

import android.app.Application
import com.flyingman.crazyreels.core.AppSettings
import com.flyingman.crazyreels.counting.ReelStore
import com.flyingman.crazyreels.debug.LayoutRecorder

class CrazyReelsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReelStore.init(this)
        LayoutRecorder.init(this)
        AppSettings.init(this)
    }
}
