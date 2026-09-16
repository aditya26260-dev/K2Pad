#ifndef K2PAD_UINPUT_BACKEND_H
#define K2PAD_UINPUT_BACKEND_H

#include <cstdint>
#include <string>

#include "uinput_syscalls.h"

namespace k2pad {

// Every function below returns 0 on success or -errno on failure — the
// same convention the underlying syscalls use, so callers get a real,
// standard POSIX error code rather than an opaque bool. Per project
// brief section 29, no step here silently swallows a failure.
//
// None of these functions open /dev/uinput themselves (except
// try_direct_open, used only for the diagnostic self-test below): the
// production path receives an already-open, privileged file descriptor
// from Phase 5/6's Shizuku integration, since Unix permission checks
// happen at open() time, not on every subsequent ioctl/write/close —
// see the Phase 3 delivery notes for the same reasoning applied to
// evdev reads.

// Attempts a direct open of /dev/uinput. Expected to fail with -EACCES
// or similar on an unprivileged app process — this exists only so the
// diagnostic self-test below can report that fact accurately, not
// because the production path relies on it.
int try_direct_open(const UinputSyscalls* sys);

// Declares every capability bit K2Pad's virtual controller reports
// (buttons, sticks, triggers, D-pad hat). Must be called before
// create_device().
int configure_capabilities(const UinputSyscalls* sys, int fd);

// Sets device identity (name + a widely-recognized Xbox 360 controller
// vendor/product ID, for maximum compatibility with games that special-
// case known controllers — project brief section 8's "widely compatible
// Xbox-style controller") and the range of every axis, then creates the
// device. Must be called after configure_capabilities().
int create_device(const UinputSyscalls* sys, int fd);

// Writes one button's state change. Does NOT send SYN_REPORT — call
// sync_report() once after writing everything that changed this update,
// matching real evdev/uinput protocol framing (see Phase 3's
// EvdevEventTranslator for the same grouping applied on the read side).
int write_key_event(const UinputSyscalls* sys, int fd, int linux_key_code, bool pressed);

// Writes one axis's absolute value. See write_key_event's SYN_REPORT note.
int write_abs_event(const UinputSyscalls* sys, int fd, int linux_abs_code, int32_t value);

// Flushes a batch of write_key_event/write_abs_event calls as one
// consistent update.
int sync_report(const UinputSyscalls* sys, int fd);

// Destroys the device. Safe to call even after a partial failure earlier
// in setup.
int destroy_device(const UinputSyscalls* sys, int fd);

// Converts K2Pad's independent dpad up/down/left/right booleans into the
// hat-switch X/Y values a real Xbox-style pad reports over ABS_HAT0X/Y —
// exactly the "convert to native ranges only at the backend boundary"
// step project brief section 26 asks for. Opposite-direction pairs held
// together cancel to 0, matching WasdStickCalculator's cancellation
// behavior on the Kotlin side.
int32_t dpad_to_hat_x(bool left, bool right);
int32_t dpad_to_hat_y(bool up, bool down);

// Runs the exact 7-step sequence project brief section 29 asks for (open,
// configure, create, test button, test stick, release, destroy) and
// returns a human-readable line-by-line report of what happened at each
// step. Never throws, never skips reporting a failure — this is the
// basis for Phase 7's "Run Backend Test" diagnostic.
std::string run_self_test(const UinputSyscalls* sys);

}  // namespace k2pad

#endif  // K2PAD_UINPUT_BACKEND_H
