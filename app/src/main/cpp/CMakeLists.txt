# Minimum version consistent with the NDK's own bundled CMake toolchain support.
cmake_minimum_required(VERSION 3.22.1)

project("k2pad_native" LANGUAGES CXX)

# Phase 1: a single tiny status-check source file, to prove the Gradle <->
# CMake <-> NDK <-> JNI chain compiles and links correctly.
#
# Phase 4 onward will add evdev_reader.cpp and uinput_writer.cpp here as
# additional sources on the SAME k2pad_native target, so Kotlin only ever
# needs one System.loadLibrary("k2pad_native") call. Nothing about that
# future work is implemented yet — no uinput/evdev headers are included
# below, on purpose, per this project's "never fabricate/never claim a
# backend exists before it does" rule.
add_library(
        k2pad_native
        SHARED
        native-lib.cpp
)

find_library(log-lib log)

target_link_libraries(
        k2pad_native
        ${log-lib}
)

target_compile_options(k2pad_native PRIVATE -Wall -Wextra)
