package com.k2pad.app.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

/**
 * App-process-side entry point to Shizuku. Checks whether the Shizuku app
 * is installed and its service reachable, requests its permission, and
 * binds [UInputUserService]. Everything here is about establishing a
 * connection — Phase 6 is what actually drives GamepadState through the
 * resulting [IUInputService] into a live virtual gamepad.
 *
 * Shizuku's own API throws RuntimeException (not RemoteException) on
 * failure as of v11+ — every call into it below is guarded accordingly,
 * so a Shizuku-side problem shows up as a reported [State], never a crash.
 */
object ShizukuManager {

    private const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE = 9721 // arbitrary; just needs to stay stable and ours

    sealed interface State {
        /** Shizuku app isn't installed at all. */
        data object NotInstalled : State
        /** Shizuku app is installed, but its service isn't reachable (not started, or just booted). */
        data object NotRunning : State
        data object PermissionNeeded : State
        data object PermissionDenied : State
        data object Connecting : State
        data class Connected(val service: IUInputService) : State
        data class Unavailable(val reason: String) : State
    }

    private var currentState: State = State.NotInstalled
    private var onStateChanged: ((State) -> Unit)? = null
    private var boundService: IUInputService? = null
    private var listenersRegistered = false
    private var lastBindArgs: Shizuku.UserServiceArgs? = null

    /** Captured once in [start] so bindUserService doesn't need a Context on every call. */
    private var applicationId: String? = null

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != REQUEST_CODE) return@OnRequestPermissionResultListener
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                bindUserService()
            } else {
                updateState(State.PermissionDenied)
            }
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            if (!binder.pingBinder()) return
            val service = IUInputService.Stub.asInterface(binder)
            boundService = service
            updateState(State.Connected(service))
        }

        override fun onServiceDisconnected(name: ComponentName) {
            boundService = null
            updateState(State.NotRunning)
        }
    }

    /**
     * Registers listeners and records the app's own package name for later
     * use by bindUserService. Call once, early (e.g. Application.onCreate)
     * — Shizuku's own guidance is to register listeners before any
     * binder-dependent call happens.
     */
    fun start(context: Context) {
        applicationId = context.packageName
        if (listenersRegistered) return
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        listenersRegistered = true
    }

    /** Unregisters listeners and unbinds the user service if one is bound. Call from the corresponding onDestroy. */
    fun stop() {
        if (listenersRegistered) {
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            listenersRegistered = false
        }
        lastBindArgs?.let { args ->
            try {
                Shizuku.unbindUserService(args, serviceConnection, true)
            } catch (t: Throwable) {
                // Best-effort: if Shizuku itself is already gone there's
                // nothing left to unbind from.
            }
        }
        boundService = null
        lastBindArgs = null
    }

    fun addListener(listener: (State) -> Unit) {
        onStateChanged = listener
        listener(currentState)
    }

    /**
     * Re-checks installation/running/permission state and, if permission is
     * already granted, starts binding the user service. Safe to call
     * repeatedly (e.g. from a "Check Shizuku" button).
     */
    fun refreshAvailability(context: Context) {
        if (!isShizukuAppInstalled(context)) {
            updateState(State.NotInstalled)
            return
        }

        val binderAlive = try {
            Shizuku.pingBinder()
        } catch (t: Throwable) {
            false
        }
        if (!binderAlive) {
            updateState(State.NotRunning)
            return
        }

        try {
            if (Shizuku.isPreV11()) {
                updateState(State.Unavailable("Shizuku is too old (pre-v11) — please update the Shizuku app"))
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                bindUserService()
            } else {
                updateState(State.PermissionNeeded)
            }
        } catch (t: Throwable) {
            updateState(State.Unavailable("checking Shizuku failed: ${t.message}"))
        }
    }

    /** Shows Shizuku's own permission prompt. The result arrives via the registered listener. */
    fun requestPermission() {
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (t: Throwable) {
            updateState(State.Unavailable("requestPermission failed: ${t.message}"))
        }
    }

    private fun bindUserService() {
        val appId = applicationId
        if (appId == null) {
            updateState(State.Unavailable("ShizukuManager.start(context) was never called"))
            return
        }
        updateState(State.Connecting)
        val args = Shizuku.UserServiceArgs(ComponentName(appId, UInputUserService::class.java.name))
            .daemon(false)
            .processNameSuffix("uinput_service")
            .debuggable(false)
            .version(1)
            .tag("k2pad-uinput-service")
        lastBindArgs = args
        try {
            Shizuku.bindUserService(args, serviceConnection)
        } catch (t: Throwable) {
            updateState(State.Unavailable("bindUserService failed: ${t.message}"))
        }
    }

    private fun isShizukuAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun updateState(newState: State) {
        currentState = newState
        onStateChanged?.invoke(newState)
    }
}
