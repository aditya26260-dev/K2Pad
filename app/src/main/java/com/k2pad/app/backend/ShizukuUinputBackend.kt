package com.k2pad.app.backend

import com.k2pad.app.mapping.GamepadState
import com.k2pad.app.nativebridge.NativeBridge
import com.k2pad.app.shizuku.IUInputService

/**
 * Drives a real virtual gamepad through Shizuku's privileged helper +
 * Phase 4's native uinput code. This is the first place in the project
 * where all of Phases 4-5 actually connect: a privileged fd from
 * [service], handed to native code that speaks the real uinput protocol.
 *
 * fd lifecycle: [android.os.ParcelFileDescriptor.detachFd] hands
 * ownership of the raw fd to native code — from that point, native code
 * (not this class, not the ParcelFileDescriptor's finalizer) is what
 * eventually closes it, in nativeUinputDestroy.
 */
class ShizukuUinputBackend(private val service: IUInputService) : VirtualGamepadBackend {

    private var fd: Int = -1

    override fun start(): String? {
        if (fd >= 0) return null // already started

        val pfd = try {
            service.openUinput()
        } catch (t: Throwable) {
            return "openUinput() call failed: ${t.message}"
        }

        if (pfd == null) {
            val errno = try {
                service.getLastErrno()
            } catch (t: Throwable) {
                -1
            }
            return "openUinput() returned null (errno=$errno) — the privileged " +
                "helper couldn't open /dev/uinput either; this device's SELinux " +
                "policy likely blocks it even for shell/root"
        }

        val rawFd = pfd.detachFd()
        val rc = NativeBridge.uinputCreate(rawFd)
        if (rc != 0) {
            return "native configure/create failed (rc=$rc)"
        }
        fd = rawFd
        return null
    }

    override fun writeState(state: GamepadState) {
        if (fd < 0) return
        NativeBridge.uinputWriteState(fd, state)
    }

    override fun stop() {
        if (fd < 0) return
        NativeBridge.uinputDestroy(fd) // releases to neutral, destroys, and closes fd
        fd = -1
    }
}
