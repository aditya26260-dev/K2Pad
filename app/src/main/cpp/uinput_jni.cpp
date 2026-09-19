#include <jni.h>

#include "uinput_backend.h"
#include "uinput_syscalls.h"

// Kotlin declaration this must match exactly (instance method on the
// `NativeBridge` Kotlin object, in package com.k2pad.app.nativebridge):
//
//   private external fun nativeRunUinputSelfTest(): String
//
// Always uses real_uinput_syscalls() — the fake used in this phase's own
// verification never ships in the app; see uinput_backend.h's doc for why
// swapping is possible at all.
extern "C" JNIEXPORT jstring JNICALL
Java_com_k2pad_app_nativebridge_NativeBridge_nativeRunUinputSelfTest(JNIEnv* env, jobject /* thiz */) {
    std::string result = k2pad::run_self_test(k2pad::real_uinput_syscalls());
    return env->NewStringUTF(result.c_str());
}

// Phase 6: the real (non-self-test) path. `fd` here is a privileged file
// descriptor obtained via Shizuku (see ShizukuUinputBackend.kt) — unlike
// nativeRunUinputSelfTest, this never opens /dev/uinput itself.

// Kotlin: private external fun nativeUinputCreate(fd: Int): Int
extern "C" JNIEXPORT jint JNICALL
Java_com_k2pad_app_nativebridge_NativeBridge_nativeUinputCreate(JNIEnv* /* env */, jobject /* thiz */, jint fd) {
    const k2pad::UinputSyscalls* sys = k2pad::real_uinput_syscalls();
    int rc = k2pad::configure_capabilities(sys, fd);
    if (rc != 0) return rc;
    return k2pad::create_device(sys, fd);
}

// Kotlin: private external fun nativeUinputWriteState(fd: Int, leftX: Float, leftY: Float,
//     rightX: Float, rightY: Float, leftTrigger: Float, rightTrigger: Float,
//     buttonsBitmask: Int, hatX: Int, hatY: Int): Int
//
// Stick/trigger values arrive here ALREADY converted to native uinput
// ranges by NativeBridge.kt (the actual "backend boundary" per project
// brief section 26) — this function only forwards them.
extern "C" JNIEXPORT jint JNICALL
Java_com_k2pad_app_nativebridge_NativeBridge_nativeUinputWriteState(
    JNIEnv* /* env */, jobject /* thiz */, jint fd,
    jint leftX, jint leftY, jint rightX, jint rightY,
    jint leftTrigger, jint rightTrigger,
    jint buttonsBitmask, jint hatX, jint hatY) {
    const k2pad::UinputSyscalls* sys = k2pad::real_uinput_syscalls();
    return k2pad::write_full_state(
        sys, fd, leftX, leftY, rightX, rightY, leftTrigger, rightTrigger, buttonsBitmask, hatX, hatY);
}

// Kotlin: private external fun nativeUinputDestroy(fd: Int): Int
extern "C" JNIEXPORT jint JNICALL
Java_com_k2pad_app_nativebridge_NativeBridge_nativeUinputDestroy(JNIEnv* /* env */, jobject /* thiz */, jint fd) {
    const k2pad::UinputSyscalls* sys = k2pad::real_uinput_syscalls();
    // Release to neutral before destroying — same failsafe principle as
    // the self-test's step 6, so nothing stays stuck pressed even if the
    // caller never explicitly zeroed the state first.
    k2pad::write_full_state(sys, fd, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    int rc = k2pad::destroy_device(sys, fd);
    sys->close_fd(fd);
    return rc;
}
