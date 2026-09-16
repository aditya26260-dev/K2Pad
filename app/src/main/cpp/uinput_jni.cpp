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
