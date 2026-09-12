#include <jni.h>
#include <string>

// Kotlin declaration this must match exactly (instance method on the
// `NativeBridge` Kotlin object, in package com.k2pad.app.nativebridge):
//
//   private external fun nativeGetStatus(): String
//
// Because it's an instance method (Kotlin `object` singletons expose their
// members as instance methods on the JVM, not static ones), the second JNI
// parameter is `jobject thiz`, not `jclass clazz`.
extern "C" JNIEXPORT jstring JNICALL
Java_com_k2pad_app_nativebridge_NativeBridge_nativeGetStatus(JNIEnv *env, jobject /* thiz */) {
    std::string status =
        "k2pad_native loaded OK (Phase 1 stub — no uinput/evdev backend yet)";
    return env->NewStringUTF(status.c_str());
}
