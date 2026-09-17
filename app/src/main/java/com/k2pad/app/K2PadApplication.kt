package com.k2pad.app

import android.app.Application
import com.k2pad.app.shizuku.ShizukuManager

/**
 * Application entry point.
 *
 * Phase 5: registers ShizukuManager's listener as early as possible, per
 * Shizuku's own guidance to do this before any binder-dependent call
 * happens — not tied to any one Activity's lifecycle, since the
 * connection should persist across screens.
 */
class K2PadApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ShizukuManager.start(this)
    }
}
