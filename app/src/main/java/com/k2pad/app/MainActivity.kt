package com.k2pad.app

import android.os.Bundle
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.k2pad.app.capture.AndroidKeyCodeMap
import com.k2pad.app.mapping.GamepadState
import com.k2pad.app.mapping.MappingEngine
import com.k2pad.app.mapping.MouseButton
import com.k2pad.app.mapping.WheelDirection
import com.k2pad.app.nativebridge.NativeBridge
import com.k2pad.app.ui.theme.K2PadTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    // Deliberately a plain field, not a ViewModel: this whole screen is a
    // temporary local-preview stopgap (see Phase3PreviewScreen doc) that
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
                    Phase3PreviewScreen(mappingEngine = mappingEngine)
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
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun Phase3PreviewScreen(mappingEngine: MappingEngine) {
    val view = LocalView.current
    var state by remember { mutableStateOf(GamepadState.NEUTRAL) }
    var mouseCaptured by remember { mutableStateOf(false) }
    var uinputTestResult by remember { mutableStateOf("(not run yet)") }
    val scrollState = rememberScrollState()

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

    // Polls the engine ~60 times/second purely to refresh this preview's
    // text. Phase 4+'s real backend loop replaces this with an actual
    // uinput write cycle — this is only here so the numbers on screen move.
    LaunchedEffect(Unit) {
        while (true) {
            mappingEngine.tick(0.016f)
            state = mappingEngine.currentState()
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
                    "this window has focus — it is not the system-wide path GTA V needs; " +
                    "that's the evdev+Shizuku capture built this phase and wired up live in " +
                    "Phase 5.",
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
        }
    }
}

private fun mouseButtonFor(actionButton: Int): MouseButton? = when (actionButton) {
    MotionEvent.BUTTON_PRIMARY -> MouseButton.LEFT
    MotionEvent.BUTTON_SECONDARY -> MouseButton.RIGHT
    MotionEvent.BUTTON_TERTIARY -> MouseButton.MIDDLE
    else -> null
}

@Preview(showBackground = true)
@Composable
fun Phase3PreviewScreenPreview() {
    K2PadTheme {
        Phase3PreviewScreen(mappingEngine = MappingEngine())
    }
}
