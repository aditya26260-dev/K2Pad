#include "uinput_backend.h"

#include <cerrno>
#include <cstring>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <sstream>

namespace k2pad {
namespace {

constexpr const char* kDevicePath = "/dev/uinput";
constexpr const char* kDeviceName = "K2Pad Virtual Controller";

// Xbox 360 Wired Controller's real USB vendor/product ID. Used
// deliberately, not arbitrarily: many games/engines special-case
// recognized controller IDs, and this is the same widely-compatible
// choice other virtual-gamepad tools in this space make (see the
// SteamController-Android precedent cited in the Phase 1 feasibility
// analysis) — it's just numbers advertised over the input subsystem, not
// a claim of being a genuine Microsoft product.
constexpr uint16_t kVendorId = 0x045e;
constexpr uint16_t kProductId = 0x028e;

const int kButtonCodes[] = {
    BTN_A, BTN_B, BTN_X, BTN_Y,
    BTN_TL, BTN_TR,
    BTN_SELECT, BTN_START,
    BTN_THUMBL, BTN_THUMBR,
};

struct AxisRange {
    int code;
    int32_t minimum;
    int32_t maximum;
};

// Stick range matches a real Xbox 360 pad's reported int16 range.
// Trigger range (0..255) matches the same. Fuzz/flat are left at 0 for
// every axis: MappingEngine already applies deadzone shaping upstream
// (see Deadzone.kt) — a second, hardware-reported deadzone here would
// just compound with it rather than add anything.
const AxisRange kAxisRanges[] = {
    {ABS_X, -32768, 32767},
    {ABS_Y, -32768, 32767},
    {ABS_RX, -32768, 32767},
    {ABS_RY, -32768, 32767},
    {ABS_Z, 0, 255},
    {ABS_RZ, 0, 255},
    {ABS_HAT0X, -1, 1},
    {ABS_HAT0Y, -1, 1},
};

std::string describe_errno(int negative_errno) {
    return std::string(strerror(-negative_errno));
}

std::string step_result(const char* label, int rc) {
    std::ostringstream out;
    if (rc == 0) {
        out << label << ": OK\n";
    } else {
        out << label << ": FAILED (" << describe_errno(rc) << ")\n";
    }
    return out.str();
}

}  // namespace

int try_direct_open(const UinputSyscalls* sys) {
    int fd = sys->open_path(kDevicePath, O_WRONLY | O_NONBLOCK);
    if (fd < 0) return -errno;
    return fd;
}

int configure_capabilities(const UinputSyscalls* sys, int fd) {
    if (sys->ioctl_int(fd, UI_SET_EVBIT, EV_KEY) < 0) return -errno;
    for (int code : kButtonCodes) {
        if (sys->ioctl_int(fd, UI_SET_KEYBIT, code) < 0) return -errno;
    }

    if (sys->ioctl_int(fd, UI_SET_EVBIT, EV_ABS) < 0) return -errno;
    for (const auto& axis : kAxisRanges) {
        if (sys->ioctl_int(fd, UI_SET_ABSBIT, axis.code) < 0) return -errno;
    }

    return 0;
}

int create_device(const UinputSyscalls* sys, int fd) {
    struct uinput_setup setup {};
    setup.id.bustype = BUS_USB;
    setup.id.vendor = kVendorId;
    setup.id.product = kProductId;
    setup.id.version = 1;
    setup.ff_effects_max = 0;  // no rumble/force-feedback — not in scope
    std::strncpy(setup.name, kDeviceName, UINPUT_MAX_NAME_SIZE - 1);

    if (sys->ioctl_ptr(fd, UI_DEV_SETUP, &setup) < 0) return -errno;

    for (const auto& axis : kAxisRanges) {
        struct uinput_abs_setup abs_setup {};
        abs_setup.code = static_cast<uint16_t>(axis.code);
        abs_setup.absinfo.minimum = axis.minimum;
        abs_setup.absinfo.maximum = axis.maximum;
        abs_setup.absinfo.fuzz = 0;
        abs_setup.absinfo.flat = 0;
        if (sys->ioctl_ptr(fd, UI_ABS_SETUP, &abs_setup) < 0) return -errno;
    }

    if (sys->ioctl_int(fd, UI_DEV_CREATE, 0) < 0) return -errno;
    return 0;
}

int write_key_event(const UinputSyscalls* sys, int fd, int linux_key_code, bool pressed) {
    struct input_event ev {};
    ev.type = EV_KEY;
    ev.code = static_cast<uint16_t>(linux_key_code);
    ev.value = pressed ? 1 : 0;
    if (sys->write_bytes(fd, &ev, sizeof(ev)) != static_cast<ssize_t>(sizeof(ev))) {
        return -errno;
    }
    return 0;
}

int write_abs_event(const UinputSyscalls* sys, int fd, int linux_abs_code, int32_t value) {
    struct input_event ev {};
    ev.type = EV_ABS;
    ev.code = static_cast<uint16_t>(linux_abs_code);
    ev.value = value;
    if (sys->write_bytes(fd, &ev, sizeof(ev)) != static_cast<ssize_t>(sizeof(ev))) {
        return -errno;
    }
    return 0;
}

int sync_report(const UinputSyscalls* sys, int fd) {
    struct input_event ev {};
    ev.type = EV_SYN;
    ev.code = SYN_REPORT;
    ev.value = 0;
    if (sys->write_bytes(fd, &ev, sizeof(ev)) != static_cast<ssize_t>(sizeof(ev))) {
        return -errno;
    }
    return 0;
}

int destroy_device(const UinputSyscalls* sys, int fd) {
    if (sys->ioctl_int(fd, UI_DEV_DESTROY, 0) < 0) return -errno;
    return 0;
}

int32_t dpad_to_hat_x(bool left, bool right) {
    if (left && right) return 0;
    if (left) return -1;
    if (right) return 1;
    return 0;
}

int32_t dpad_to_hat_y(bool up, bool down) {
    if (up && down) return 0;
    if (up) return -1;
    if (down) return 1;
    return 0;
}

int write_full_state(
    const UinputSyscalls* sys, int fd,
    int32_t left_x, int32_t left_y, int32_t right_x, int32_t right_y,
    int32_t left_trigger, int32_t right_trigger,
    int32_t buttons_bitmask, int32_t hat_x, int32_t hat_y) {
    int rc;

    rc = write_abs_event(sys, fd, ABS_X, left_x);
    if (rc != 0) return rc;
    rc = write_abs_event(sys, fd, ABS_Y, left_y);
    if (rc != 0) return rc;
    rc = write_abs_event(sys, fd, ABS_RX, right_x);
    if (rc != 0) return rc;
    rc = write_abs_event(sys, fd, ABS_RY, right_y);
    if (rc != 0) return rc;
    rc = write_abs_event(sys, fd, ABS_Z, left_trigger);
    if (rc != 0) return rc;
    rc = write_abs_event(sys, fd, ABS_RZ, right_trigger);
    if (rc != 0) return rc;
    rc = write_abs_event(sys, fd, ABS_HAT0X, hat_x);
    if (rc != 0) return rc;
    rc = write_abs_event(sys, fd, ABS_HAT0Y, hat_y);
    if (rc != 0) return rc;

    // kButtonCodes' order (A,B,X,Y,LB,RB,BACK,START,L3,R3) is the contract
    // Kotlin's packButtons() in NativeBridge.kt packs bits in — change one,
    // change the other.
    for (size_t i = 0; i < sizeof(kButtonCodes) / sizeof(kButtonCodes[0]); i++) {
        bool pressed = (buttons_bitmask & (1 << i)) != 0;
        rc = write_key_event(sys, fd, kButtonCodes[i], pressed);
        if (rc != 0) return rc;
    }

    return sync_report(sys, fd);
}

std::string run_self_test(const UinputSyscalls* sys) {
    std::ostringstream out;

    int fd = try_direct_open(sys);
    if (fd < 0) {
        out << step_result("1) open /dev/uinput", fd);
        out << "   FAILURE HERE IS EXPECTED for an unprivileged app process.\n"
               "   Phase 5/6 supplies a privileged fd via Shizuku instead of\n"
               "   this direct open — this step exists only to report what\n"
               "   this specific device/process actually does.\n";
        return out.str();
    }
    out << "1) open /dev/uinput: OK (fd=" << fd << ")\n";

    int rc = configure_capabilities(sys, fd);
    out << step_result("2) configure capabilities", rc);
    if (rc != 0) {
        sys->close_fd(fd);
        return out.str();
    }

    rc = create_device(sys, fd);
    out << step_result("3) create device", rc);
    if (rc != 0) {
        sys->close_fd(fd);
        return out.str();
    }

    rc = write_key_event(sys, fd, BTN_A, true);
    out << step_result("4) write test button (A down)", rc);

    rc = write_abs_event(sys, fd, ABS_X, 16000);
    out << step_result("5) write test stick (left X = 16000)", rc);

    rc = sync_report(sys, fd);
    out << step_result("   sync report", rc);

    // Release everything back to neutral before destroying, regardless
    // of whether the writes above succeeded — matches the failsafe
    // principle (project brief section 20) of never leaving state stuck.
    write_key_event(sys, fd, BTN_A, false);
    write_abs_event(sys, fd, ABS_X, 0);
    sync_report(sys, fd);
    out << "6) release to neutral: attempted\n";

    rc = destroy_device(sys, fd);
    out << step_result("7) destroy device", rc);

    sys->close_fd(fd);
    return out.str();
}

}  // namespace k2pad
