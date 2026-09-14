package com.k2pad.app.capture

/**
 * Real Linux evdev event/key/button/axis codes this project needs.
 *
 * Every value below was read directly from the kernel UAPI header
 * (linux/input-event-codes.h) by installing linux-libc-dev and grepping
 * the actual header — not recalled from memory — since a wrong constant
 * here would silently misread every event rather than fail loudly.
 *
 * The struct layout constants were verified by compiling a small C
 * program against linux/input.h on a real (64-bit) system:
 * `struct input_event` is 24 bytes total — a 16-byte `struct timeval
 * time` (two 8-byte longs), then u16 `type` at offset 16, u16 `code` at
 * offset 18, s32 `value` at offset 20. This is the standard layout for
 * every 64-bit Linux/Android device, which is what this project targets
 * exclusively (see README) — but per this project's own rule of
 * verifying device-dependent behavior rather than assuming it, Phase 5's
 * on-device testing against the POCO Pad 5G's actual kernel is the real
 * confirmation, not this sandbox.
 */
object EvdevCodes {
    // struct input_event layout (64-bit / modern Android).
    const val STRUCT_INPUT_EVENT_SIZE = 24
    const val OFFSET_TYPE = 16
    const val OFFSET_CODE = 18
    const val OFFSET_VALUE = 20

    // Event types.
    const val EV_SYN = 0x00
    const val EV_KEY = 0x01
    const val EV_REL = 0x02

    // EV_SYN codes.
    const val SYN_REPORT = 0

    // EV_KEY value semantics.
    const val KEY_STATE_UP = 0
    const val KEY_STATE_DOWN = 1
    const val KEY_STATE_REPEAT = 2

    // Keyboard key codes used by K2Pad's default bindings (project brief
    // sections 9 and 17). Every value below is exactly what
    // linux/input-event-codes.h defines.
    const val KEY_ESC = 1
    const val KEY_1 = 2
    const val KEY_2 = 3
    const val KEY_3 = 4
    const val KEY_4 = 5
    const val KEY_TAB = 15
    const val KEY_W = 17
    const val KEY_E = 18
    const val KEY_R = 19
    const val KEY_ENTER = 28
    const val KEY_LEFTCTRL = 29
    const val KEY_A = 30
    const val KEY_S = 31
    const val KEY_D = 32
    const val KEY_F = 33
    const val KEY_LEFTSHIFT = 42
    const val KEY_SPACE = 57
    const val KEY_UP = 103
    const val KEY_LEFT = 105
    const val KEY_RIGHT = 106
    const val KEY_DOWN = 108

    // Mouse buttons — also reported as EV_KEY events, just with codes in
    // the BTN_MOUSE range instead of the keyboard KEY_* range.
    const val BTN_LEFT = 0x110
    const val BTN_RIGHT = 0x111
    const val BTN_MIDDLE = 0x112
    const val BTN_SIDE = 0x113
    const val BTN_EXTRA = 0x114

    // Relative axes (EV_REL).
    const val REL_X = 0x00
    const val REL_Y = 0x01
    const val REL_HWHEEL = 0x06
    const val REL_WHEEL = 0x08
}
