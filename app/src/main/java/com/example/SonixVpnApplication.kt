package com.example

import android.app.Application
import com.yahorzabotsin.openvpnclientgate.core.di.coreModule
import de.blinkt.openvpn.core.GlobalPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Initializes only the VPN engine dependencies. We deliberately do not use the upstream
 * CoreApp because it also changes app locale and orientation, which must remain SONIX TV-owned.
 */
class SonixVpnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@SonixVpnApplication)
                modules(coreModule)
            }
        }
        GlobalPreferences.setInstance(false, false, false)
    }
}
