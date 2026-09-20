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
    ParcelFileDescriptor openUinput() = 1;

    /**
     * Privileged open of a specific /dev/input/eventX node (O_RDONLY |
     * O_NONBLOCK) for reading the physical keyboard/mouse. Returns null on
     * failure — see getLastErrno().
     */
    ParcelFileDescriptor openEvdevDevice(String path) = 2;

    /** errno from the most recent open*() call on THIS service instance, or 0 if it succeeded. */
    int getLastErrno() = 3;

    /**
     * Dumps recent logcat output (all buffers) from inside this privileged
     * process. Added specifically to diagnose the "remote process
     * probably died" failure on openUinput() without needing ADB/a
     * computer — shell/root UID can read logs a normal K2Pad app process
     * cannot. Filtering the (likely large) output down to what matters is
     * done on the app side, not here, so this stays a dumb passthrough.
     */
    String readRecentLog() = 4;

    // Shizuku calls this via a hardcoded transaction code when it needs to
    // tear the user-service process down (e.g. on version mismatch or
    // unbind) and can't guarantee a normal typed call will reach it. The
    // "= 16777114" here is not arbitrary — it's the exact value confirmed
    // both in Shizuku's own README and, verbatim, in its official demo
    // module's IUserService.aidl (RikkaApps/Shizuku-API, demo/src/main/aidl).
    //
    // AIDL rule that bit me here: once ANY method in an interface has an
    // explicit "= N" id, EVERY method needs one ("You must either assign
    // id's to all methods or to none of them") — confirmed directly by the
    // compiler error, and matches what both real reference files
    // (IShizukuService.aidl and the demo's IUserService.aidl) actually do:
    // every single method in each has an explicit id, not just destroy().
    void destroy() = 16777114;
}
