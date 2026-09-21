package com.k2pad.app.shizuku

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log

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

    companion object {
        // Distinctive tag, deliberately logged at ERROR level for every
        // step (not just failures) so nothing gets filtered out — this
        // exists because a first attempt at reading generic system logs
        // (AndroidRuntime/libc/avc:) showed no crash trace at all for a
        // reported "remote process probably died" failure, meaning
        // whatever happens either isn't a normal JVM-reported crash or
        // happens somewhere generic filtering missed. Explicit breadcrumbs
        // around the one call that matters (Os.open) pin down exactly how
        // far execution gets, rather than continuing to guess.
        private const val TAG = "K2PadUinputSvc"
    }

    @Volatile
    private var lastErrno: Int = 0

    override fun openUinput(): ParcelFileDescriptor? =
        openPath("/dev/uinput", OsConstants.O_WRONLY or OsConstants.O_NONBLOCK)

    override fun openEvdevDevice(path: String): ParcelFileDescriptor? =
        openPath(path, OsConstants.O_RDONLY or OsConstants.O_NONBLOCK)

    override fun getLastErrno(): Int = lastErrno

    override fun readRecentLog(): String {
        return try {
            // -t 3000, not 500: a real on-device test showed this HyperOS
            // build logs heavily even for routine touch/window events (see
            // MainActivity's filter comment) — 500 lines was rotating past
            // the actual crash before this could be read.
            val process = ProcessBuilder("logcat", "-d", "-b", "all", "-t", "3000")
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "logcat failed: ${e.message}"
        }
    }

    private fun openPath(path: String, flags: Int): ParcelFileDescriptor? {
        Log.e(TAG, "openPath($path): entered, about to call Os.open")
        var fd: java.io.FileDescriptor? = null
        return try {
            fd = Os.open(path, flags, 0)
            Log.e(TAG, "openPath($path): Os.open returned successfully")
            lastErrno = 0
            // dup() gives the returned ParcelFileDescriptor its own
            // underlying descriptor; this process's own `fd` isn't needed
            // after that, so it's closed explicitly rather than left open
            // for this (potentially long-lived, daemon(false) but
            // multi-call) helper process's whole lifetime.
            val result = ParcelFileDescriptor.dup(fd)
            Log.e(TAG, "openPath($path): ParcelFileDescriptor.dup succeeded, returning to caller")
            result
        } catch (e: ErrnoException) {
            // Never swallowed: the real errno is preserved exactly, for
            // getLastErrno() to report — same "no silent failures"
            // requirement as the native uinput self-test (project brief
            // section 29), just crossing a Binder boundary instead of a
            // JNI one.
            Log.e(TAG, "openPath($path): ErrnoException, errno=${e.errno}", e)
            lastErrno = e.errno
            null
        } catch (e: java.io.IOException) {
            // ParcelFileDescriptor.dup() itself can throw IOException
            // (distinct from ErrnoException) if the dup fails — caught
            // separately so it's reported instead of crashing this
            // process, but there's no real errno to surface for it.
            Log.e(TAG, "openPath($path): IOException from dup()", e)
            lastErrno = -1
            null
        } catch (e: Exception) {
            // Defensive final net: if this is hit at all, it means the
            // failure seen on-device ("remote process probably died") was
            // a genuine Kotlin exception this method didn't anticipate,
            // NOT a lower-level process kill (a real signal-based kill —
            // e.g. from a seccomp/SELinux policy that terminates the
            // process outright rather than returning EACCES — would not
            // be catchable here at all: if the log shows "entered" for
            // this path but NEVER shows this line, "Os.open returned", OR
            // any other exception line, that silence itself is the
            // evidence of a signal-level kill happening inside Os.open).
            Log.e(TAG, "openPath($path): unexpected Exception", e)
            lastErrno = -1
            null
        } finally {
            Log.e(TAG, "openPath($path): finally block reached, fd=$fd")
            if (fd != null) {
                try {
                    Os.close(fd)
                    Log.e(TAG, "openPath($path): closed this process's own copy of fd")
                } catch (e: ErrnoException) {
                    // Already gone; nothing left to do.
                }
            }
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
        Log.e(TAG, "destroy() called")
        System.exit(0)
    }
}
