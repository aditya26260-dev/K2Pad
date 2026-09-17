package com.k2pad.app.shizuku

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants

/**
 * Runs as shell (UID 2000, if Shizuku was started via ADB) or root (UID 0),
 * in a separate process Shizuku itself starts — per Shizuku's own docs,
 * this is "not a valid Android application process": most Context-based
 * APIs won't work here, which is fine, because this class does exactly one
 * job and nothing else: open() a device node this elevated UID can reach,
 * then hand the resulting file descriptor back to the unprivileged app
 * process via a ParcelFileDescriptor (which supports exactly this kind of
 * FD-passing over Binder). Every subsequent read/write/ioctl on that fd
 * happens back in the app process's own native code (Phase 3/4) — no
 * privilege is needed for that part, only for the initial open.
 *
 * Must have a public no-arg constructor: Shizuku instantiates this class
 * directly by name (see ShizukuManager's UserServiceArgs) — nothing else
 * in the app should ever construct it.
 */
class UInputUserService : IUInputService.Stub() {

    @Volatile
    private var lastErrno: Int = 0

    override fun openUinput(): ParcelFileDescriptor? =
        openPath("/dev/uinput", OsConstants.O_WRONLY or OsConstants.O_NONBLOCK)

    override fun openEvdevDevice(path: String): ParcelFileDescriptor? =
        openPath(path, OsConstants.O_RDONLY or OsConstants.O_NONBLOCK)

    override fun getLastErrno(): Int = lastErrno

    private fun openPath(path: String, flags: Int): ParcelFileDescriptor? {
        return try {
            val fd = Os.open(path, flags, 0)
            lastErrno = 0
            ParcelFileDescriptor.dup(fd)
        } catch (e: ErrnoException) {
            // Never swallowed: the real errno is preserved exactly, for
            // getLastErrno() to report — same "no silent failures"
            // requirement as the native uinput self-test (project brief
            // section 29), just crossing a Binder boundary instead of a
            // JNI one.
            lastErrno = e.errno
            null
        }
    }

    /**
     * Shizuku calls this (see the "= 16777114" note in IUInputService.aidl)
     * when it needs this process to clean up and exit. There's no open
     * file descriptor state held IN this service to release — every fd it
     * has ever returned is owned by whichever app-process caller received
     * it — so cleanup here is just exiting the process itself.
     */
    override fun destroy() {
        System.exit(0)
    }
}
