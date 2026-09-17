// AIDL for the Shizuku-hosted privileged helper (project brief section 25's
// ShizukuBackend / Phase 5). Deliberately tiny: the ONLY thing that needs
// shell/root privilege is opening these two device nodes — everything
// downstream (reading evdev events, writing uinput events) happens on the
// resulting file descriptor from the app's own unprivileged process, per
// the FD-handoff reasoning in Phase 3/4's docs (Unix permission checks
// happen at open() time, not on every subsequent read/write/ioctl).
package com.k2pad.app.shizuku;

interface IUInputService {

    /**
     * Privileged open of /dev/uinput (O_WRONLY | O_NONBLOCK). Returns null
     * on failure — call getLastErrno() immediately after to find out why
     * (e.g. EACCES if even the shell/root UID can't reach it on this
     * device's SELinux policy).
     */
    ParcelFileDescriptor openUinput();

    /**
     * Privileged open of a specific /dev/input/eventX node (O_RDONLY |
     * O_NONBLOCK) for reading the physical keyboard/mouse. Returns null on
     * failure — see getLastErrno().
     */
    ParcelFileDescriptor openEvdevDevice(String path);

    /** errno from the most recent open*() call on THIS service instance, or 0 if it succeeded. */
    int getLastErrno();

    // Shizuku calls this via a hardcoded transaction code when it needs to
    // tear the user-service process down (e.g. on version mismatch or
    // unbind) and can't guarantee a normal typed call will reach it. The
    // "= 16777114" here is not arbitrary — it's the exact value confirmed
    // both in Shizuku's own README and, verbatim, in its official demo
    // module's IUserService.aidl (RikkaApps/Shizuku-API, demo/src/main/aidl).
    void destroy() = 16777114;
}
