# Minimum version consistent with the NDK's own bundled CMake toolchain support.
cmake_minimum_required(VERSION 3.22.1)

project("k2pad_native" LANGUAGES CXX)

# Phase 1 added a single tiny status-check source file, proving the
# Gradle <-> CMake <-> NDK <-> JNI chain compiles and links correctly.
#
# Phase 4 adds the native uinput backend: the real Linux uinput protocol
# (open/configure/create/write/destroy), verified against the actual
# kernel headers rather than recalled from memory (see uinput_backend.cpp's
# doc comments for specifics), plus a syscall-injectable design that keeps
# the protocol logic testable without a real kernel uinput driver present.
#
# Everything still lands on the SAME k2pad_native target, so Kotlin only
# ever needs one System.loadLibrary("k2pad_native") call. Phase 5/6 will
# add the Shizuku-obtained privileged file descriptor and the evdev-side
# native sources; nothing about that is implemented here yet.
add_library(
        k2pad_native
        SHARED
        native-lib.cpp
        uinput_syscalls_real.cpp
        uinput_backend.cpp
        uinput_jni.cpp
)

find_library(log-lib log)

target_link_libraries(
        k2pad_native
        ${log-lib}
)

target_compile_options(k2pad_native PRIVATE -Wall -Wextra)
