# K2Pad

Turn a physical keyboard + mouse into a **real, Android-visible virtual game
controller** — not a touchscreen keymapper — so games that support physical
controllers but not keyboard/mouse (GTA V and similar Android ports) become
playable with proper keyboard/mouse ergonomics.

```
Physical Keyboard + Mouse → K2Pad → REAL Virtual Gamepad → Android Input System → Game
```

**Status: Phase 1 of 10 — Repository foundation.** This build proves the
toolchain (Gradle, Kotlin, Compose/Material3, CMake/NDK/JNI) compiles and
runs together. It does **not** yet capture keyboard/mouse input, talk to
Shizuku, or create a virtual gamepad — see [Roadmap](#roadmap).

## Target device

Primary dev/test device: POCO Pad 5G, Snapdragon 7s Gen 2, 8 GB RAM, **Android
16**, HyperOS, 16:10 tablet. Designed to also work on other Android 8.0+
(API 26+) devices where the backend permits — see [Feasibility](#feasibility--backend-support).

## Architecture

```
Keyboard + Mouse (USB / BT / 2.4GHz)
        │  raw EV_KEY / EV_REL
        ▼
/dev/input/eventX  (read via Shizuku's shell-UID privilege; EVIOCGRAB planned
                     for exclusive capture so the physical devices stop also
                     driving Android's normal input pipeline while active)
        │
        ▼
Shizuku UserService — runs as shell UID, hosts the JNI evdev reader +
                       uinput writer (one native library)
        │  AIDL/Binder
        ▼
K2Pad app (unprivileged) — MappingEngine
   WASD → normalized left stick (diagonals normalized, e.g. W+D ≈ (0.707,-0.707))
   mouse Δx/Δy → right stick (sensitivity/acceleration/smoothing/deadzone/curve)
   buttons + wheel → profile-driven lookup
        │
        ▼
GamepadState (canonical, normalized internal model)
        │
        ▼
back through the same UserService → uinput writer
        │
        ▼
/dev/uinput → kernel → new InputDevice "K2Pad Virtual Controller"
              (SOURCE_GAMEPAD | SOURCE_JOYSTICK, Xbox-360-class report)
        │
        ▼
GTA V / any controller-aware Android game
```

Only the Shizuku UserService touches privileged/native code directly — the UI
and mapping logic never manipulate uinput/evdev file descriptors themselves.

## Feasibility & backend support

Whether the privileged backend (Shizuku shell UID → `/dev/uinput` and
`/dev/input/eventX`) actually works depends on this specific device/ROM's
SELinux policy, and is **not yet verified on the POCO Pad 5G**. Phase 7 adds a
Diagnostics screen that reports one of:

- `SUPPORTED` — real virtual gamepad created, Android enumerates it
- `PARTIALLY SUPPORTED` — some capability blocked, degraded fallback in use
- `BACKEND BLOCKED` — privileged backend unavailable, clearly reported as such
- `UNTESTED` — not yet checked on this device

K2Pad never fakes a `SUPPORTED` result.

## Build tooling

| Tool | Version | Source / rationale |
|---|---|---|
| Android Gradle Plugin | 9.3.0 | Current stable is 9.4.0 (~1 week old as of writing); 9.3.0 is a couple of months field-tested and already supports up to API 37 |
| Gradle | 9.5.0 | AGP 9.3.0's documented minimum **and** default required version |
| Kotlin | 2.4.0 | Current stable (released July 14, 2026); 2.5.0 isn't due until December 2026 |
| JDK | 17 | AGP 9.3.0's documented minimum/default |
| compileSdk | 37 (Android 17) | Compose 1.12 (in the BOM below) itself compiles against API 37 |
| targetSdk | 36 (Android 16) | Matches the dev device's own OS version; held one level behind compileSdk on purpose — API 37 removes the opt-out for large-screen orientation/resizability enforcement, which we don't want to fight while the core engine (Phases 2-6) is still being built |
| minSdk | 26 (Android 8.0) | Broad coverage; also the minimum API for adaptive launcher icons, which this repo uses |
| NDK | 28.2.13676358 | AGP 9.3.0's documented default side-by-side NDK version |
| CMake | not pinned | Left to the AGP/SDK-managed default rather than guessed — see comment in `app/build.gradle.kts` |
| Compose BOM | 2026.08.00 | Current stable (Jetpack Compose Aug 2026 release) |

`androidx.core`, `androidx.lifecycle`, `androidx.activity`, and the test
libraries in `gradle/libs.versions.toml` are reasonable current estimates, not
individually re-verified line by line the way the toolchain versions above
were. Expect Android Studio's first sync to suggest small patch bumps for
those — accepting them is expected and safe.

The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`)
in this repo is the genuine, unmodified wrapper fetched directly from Gradle's
own `v9.5.0` release tag — not hand-reconstructed — so it should work as-is.

## Building

**Primary path — Android Studio** (needed regardless, to resolve the Android
SDK platform, build-tools, and the pinned NDK/CMake side-by-side packages):

1. Open the cloned `K2Pad/` folder in Android Studio (current stable channel).
2. Let it sync — first sync downloads the NDK and may take a few minutes.
3. Run ▶ on a device/emulator, or `Build > Build Bundle(s)/APK(s) > Build APK(s)`.

**Command line** (once Android Studio has synced at least once, or if you have
the SDK/NDK components installed some other way):

```
./gradlew assembleDebug
./gradlew test
```

**Termux**: fine for the git/unzip/copy workflow below, but Termux alone does
not include the Android SDK/NDK, so `./gradlew assembleDebug` will not
succeed purely inside Termux unless you separately install SDK components
there. Android Studio is the recommended build path; Termux is the recommended
repo-management path.

**CI**: `.github/workflows/android-ci.yml` runs `./gradlew test` and
`./gradlew assembleDebug` on GitHub's own infrastructure (full internet
access), giving an authoritative build signal independent of both the
generating sandbox and your local machine.

## Roadmap

1. **Repository foundation** (this phase) — buildable skeleton, no
   input/mapping/backend logic yet.
2. GamepadState + mapping engine + unit tests
3. Keyboard/mouse capture
4. Native uinput backend
5. Shizuku/privileged helper integration
6. Real virtual gamepad creation
7. Controller tester + diagnostics
8. GTA V profile + mouse aiming
9. Profile manager
10. UI polish + performance optimization + CI hardening + full docs

## License

MIT (see `LICENSE`) — a default choice since none was specified; trivial to
swap for Apache-2.0, GPL, or a proprietary/all-rights-reserved notice if you'd
rather use one of those instead.
