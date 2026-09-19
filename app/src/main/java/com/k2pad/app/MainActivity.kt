package com.k2pad.app

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent as AndroidKeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.k2pad.app.backend.ShizukuUinputBackend
import com.k2pad.app.backend.VirtualGamepadBackend
import com.k2pad.app.capture.AndroidKeyCodeMap
import com.k2pad.app.mapping.GamepadState
import com.k2pad.app.mapping.MappingEngine
import com.k2pad.app.mapping.MouseButton
import com.k2pad.app.mapping.WheelDirection
import com.k2pad.app.nativebridge.NativeBridge
import com.k2pad.app.shizuku.ShizukuManager
import com.k2pad.app.ui.theme.K2PadTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    // Deliberately a plain field, not a ViewModel: this whole screen is a
    // temporary local-preview stopgap (see DevPreviewScreen doc) that
    // Phase 6+ replaces outright, so it isn't worth surviving rotation.
    private val mappingEngine = MappingEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            K2PadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DevPreviewScreen(mappingEngine = mappingEngine)
                }
            }
        }
    }

    // The Activity sees every physical key event delivered to this
    // window regardless of which Compose node has internal focus, which
    // is simpler and more reliable than Compose's own focus-based key
    // API for this purpose. IMPORTANT: this only fires while K2Pad's own
    // window has focus — it is NOT the system-wide capture the project
    // brief's section 14 requires. Phase 5's real evdev+Shizuku path
    // (EvdevInputSource + EvdevEventTranslator, already built and unit
    // tested this phase) is what will actually work while GTA V is
    // foreground; this override exists only so today, with just a
    // keyboard/mouse and this app open, you can see the same MappingEngine
    // logic respond to real hardware.
    override fun dispatchKeyEvent(event: AndroidKeyEvent): Boolean {
        val logicalKey = AndroidKeyCodeMap.toLogicalKey(event.keyCode)
        if (logicalKey != null) {
            when (event.action) {
                AndroidKeyEvent.ACTION_DOWN -> mappingEngine.onKeyDown(logicalKey)
                AndroidKeyEvent.ACTION_UP -> mappingEngine.onKeyUp(logicalKey)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        // Only unbinds/unregisters if this Activity is actually finishing,
        // not on a config-change recreation — Shizuku's connection is
        // meant to outlive individual screens, which is why start() lives
        // in K2PadApplication rather than here.
        if (isFinishing) {
            ShizukuManager.stop()
        }
        super.onDestroy()
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DevPreviewScreen(mappingEngine: MappingEngine) {
    val view = LocalView.current
    val context = LocalContext.current
    var state by remember { mutableStateOf(GamepadState.NEUTRAL) }
    var mouseCaptured by remember { mutableStateOf(false) }
    var uinputTestResult by remember { mutableStateOf("(not run yet)") }
    var shizukuState by remember { mutableStateOf<ShizukuManager.State>(ShizukuManager.State.NotInstalled) }
    var backend by remember { mutableStateOf<VirtualGamepadBackend?>(null) }
    var backendStatus by remember { mutableStateOf("(not started)") }
    var deviceListResult by remember { mutableStateOf("(not checked)") }
    val scrollState = rememberScrollState()

    DisposableEffect(Unit) {
        // ShizukuManager currently supports only one active listener at a
        // time (see its addListener doc) — fine for this single-screen
        // app; a future multi-screen Diagnostics UI (Phase 7) would need
        // ShizukuManager to support a real list of listeners instead.
        ShizukuManager.addListener { shizukuState = it }
        onDispose {
            // Leaving this screen (not just backgrounding — Compose keeps
            // this alive across a simple app-switch) tears down the real
            // gamepad if one was started, matching the failsafe principle
            // of never leaving state stuck once nothing is driving it.
            backend?.stop()
        }
    }

    // Mouse: View.requestPointerCapture() + OnCapturedPointerListener is
    // the current, documented Android API for exclusive/relative mouse
    // input (developer.android.com/games/playgames/input-mouse) — while
    // captured, MotionEvent.getX()/getY() report relative deltas instead
    // of absolute screen position, which is exactly what onMouseDelta
    // wants and avoids the edge-of-screen clamping problem absolute
    // coordinates would have.
    DisposableEffect(view) {
        view.setOnCapturedPointerListener { _, motionEvent ->
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_MOVE ->
                    mappingEngine.onMouseDelta(motionEvent.x, motionEvent.y)

                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_BUTTON_PRESS ->
                    mouseButtonFor(motionEvent.actionButton)?.let { mappingEngine.onMouseButtonDown(it) }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_BUTTON_RELEASE ->
                    mouseButtonFor(motionEvent.actionButton)?.let { mappingEngine.onMouseButtonUp(it) }

                MotionEvent.ACTION_SCROLL -> {
                    val scrollY = motionEvent.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    if (scrollY > 0f) mappingEngine.onWheel(WheelDirection.UP)
                    else if (scrollY < 0f) mappingEngine.onWheel(WheelDirection.DOWN)
                }
            }
            true
        }
        onDispose { view.setOnCapturedPointerListener(null) }
    }

    // Polls the engine ~60 times/second: refreshes this preview's text, and
    // — the actual point of Phase 6 — pushes the same state to a real
    // virtual gamepad once one has been started below.
    LaunchedEffect(Unit) {
        while (true) {
            mappingEngine.tick(0.016f)
            state = mappingEngine.currentState()
            backend?.writeState(state)
            delay(16L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("K2Pad") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Phase 3: Capture Preview (local only)",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "WASD/keys and a captured mouse feed the real MappingEngine below, " +
                    "using Android's own Activity-focus input APIs. This works ONLY while " +
                    "this window has focus — it is not the system-wide path GTA V needs. " +
                    "The real evdev+Shizuku capture (EvdevInputSource, built in Phase 3) " +
                    "still isn't wired to a live device here — Phase 6 wired up the OUTPUT " +
                    "half (uinput) below; live system-wide input capture is still open.",
                style = MaterialTheme.typography.bodyLarge
            )

            Button(onClick = {
                if (mouseCaptured) {
                    view.releasePointerCapture()
                } else {
                    view.requestPointerCapture()
                }
                mouseCaptured = !mouseCaptured
            }) {
                Text(if (mouseCaptured) "Release mouse" else "Capture mouse")
            }

            Text(
                text = "Left stick:  (%.2f, %.2f)".format(state.leftStickX, state.leftStickY),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Right stick: (%.2f, %.2f)".format(state.rightStickX, state.rightStickY),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Triggers: LT=%.1f RT=%.1f".format(state.leftTrigger, state.rightTrigger),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Buttons: " + buildString {
                    if (state.a) append("A ")
                    if (state.b) append("B ")
                    if (state.x) append("X ")
                    if (state.y) append("Y ")
                    if (state.lb) append("LB ")
                    if (state.rb) append("RB ")
                    if (state.back) append("BACK ")
                    if (state.start) append("START ")
                    if (isEmpty()) append("(none)")
                },
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider()

            Text(
                text = "Phase 4: Native uinput self-test",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Runs the real open/configure/create/write/destroy uinput sequence " +
                    "on this device via a direct (unprivileged) open of /dev/uinput. A " +
                    "permission failure at step 1 is an expected, useful result here, not " +
                    "a bug — it's this device telling us it needs the privileged path " +
                    "(Shizuku, Phase 5) rather than a plain app process.",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = { uinputTestResult = NativeBridge.runUinputSelfTest() }) {
                Text("Run native uinput self-test")
            }
            Text(
                text = uinputTestResult,
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider()

            Text(
                text = "Phase 5: Shizuku",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Checks whether the Shizuku app is installed and reachable, and " +
                    "requests its permission. On grant, K2Pad binds its privileged helper " +
                    "(a separate process Shizuku starts as shell/root) — that's what " +
                    "actually opens /dev/uinput on your device's behalf below. Evdev " +
                    "capture (opening the keyboard/mouse's own device nodes the same way) " +
                    "isn't wired up yet — Phase 6 below only used this connection for the " +
                    "uinput/output side.",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = { ShizukuManager.refreshAvailability(context) }) {
                Text("Check Shizuku")
            }
            if (shizukuState is ShizukuManager.State.PermissionNeeded) {
                Button(onClick = { ShizukuManager.requestPermission() }) {
                    Text("Request Shizuku permission")
                }
            }
            Text(
                text = "Status: " + describeShizukuState(shizukuState),
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider()

            Text(
                text = "Phase 6: Real virtual gamepad",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Takes the Shizuku connection above, actually calls openUinput() " +
                    "through it, and hands the resulting privileged fd to Phase 4's native " +
                    "code. If this creates the device, the tick loop above starts pushing " +
                    "the real WASD/mouse state to it — this is the first point in the whole " +
                    "project where we find out if a real virtual gamepad genuinely works on " +
                    "this device, not just each piece in isolation.",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = {
                val active = backend
                if (active != null) {
                    active.stop()
                    backend = null
                    backendStatus = "Stopped"
                } else {
                    val connected = shizukuState as? ShizukuManager.State.Connected
                    if (connected == null) {
                        backendStatus = "Shizuku isn't connected — use Check Shizuku above first"
                    } else {
                        val newBackend = ShizukuUinputBackend(connected.service)
                        val error = newBackend.start()
                        if (error == null) {
                            backend = newBackend
                            backendStatus = "Started — try 'List input devices' below, or " +
                                "switch to another app to check for a new controller"
                        } else {
                            backendStatus = "Failed to start: $error"
                        }
                    }
                }
            }) {
                Text(if (backend != null) "Stop virtual controller" else "Start virtual controller")
            }
            Text(
                text = "Status: $backendStatus",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = {
                val lines = InputDevice.getDeviceIds().toList().mapNotNull { id ->
                    InputDevice.getDevice(id)?.let { device ->
                        val isGamepadOrJoystick =
                            (device.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                                (device.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                        device.name + if (isGamepadOrJoystick) "  [gamepad/joystick source]" else ""
                    }
                }
                deviceListResult = if (lines.isEmpty()) {
                    "(no input devices reported)"
                } else {
                    lines.joinToString("\n")
                }
            }) {
                Text("List input devices")
            }
            Text(
                text = deviceListResult,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun describeShizukuState(state: ShizukuManager.State): String = when (state) {
    is ShizukuManager.State.NotInstalled -> "Shizuku app not installed"
    is ShizukuManager.State.NotRunning -> "Shizuku installed but not running — open it and tap Start"
    is ShizukuManager.State.PermissionNeeded -> "Permission not yet granted"
    is ShizukuManager.State.PermissionDenied -> "Permission denied"
    is ShizukuManager.State.Connecting -> "Connecting…"
    is ShizukuManager.State.Connected -> "Connected — privileged helper is bound and ready"
    is ShizukuManager.State.Unavailable -> "Unavailable: ${state.reason}"
}

private fun mouseButtonFor(actionButton: Int): MouseButton? = when (actionButton) {
    MotionEvent.BUTTON_PRIMARY -> MouseButton.LEFT
    MotionEvent.BUTTON_SECONDARY -> MouseButton.RIGHT
    MotionEvent.BUTTON_TERTIARY -> MouseButton.MIDDLE
    else -> null
}

@Preview(showBackground = true)
@Composable
fun DevPreviewScreenPreview() {
    K2PadTheme {
        DevPreviewScreen(mappingEngine = MappingEngine())
    }
}
