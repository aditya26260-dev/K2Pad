package com.k2pad.app

import android.app.Application

/**
 * Application entry point.
 *
 * Intentionally minimal in Phase 1. From Phase 5 onward this is where the
 * Shizuku connection listener gets registered (Shizuku recommends binding
 * that listener as early as possible, before any Activity is created), and
 * later where ProfileManager loads the last-used profile from disk.
 */
class K2PadApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Nothing yet — see class doc above for what lands here in later phases.
    }
}
