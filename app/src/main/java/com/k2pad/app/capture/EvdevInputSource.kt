package com.k2pad.app.capture

import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs a background read loop over an already-open evdev stream, parsing
 * each fixed-size chunk into a [RawInputEvent] and handing it to [onEvent].
 *
 * Deliberately not tied to any specific way of opening the stream. In
 * production this wraps a real /dev/input/eventX file descriptor —
 * Phase 5 is what actually performs the privileged open via Shizuku and
 * hands the resulting descriptor here; this class doesn't know or care
 * how it was obtained. That decoupling is what makes it fully testable
 * today with a plain in-memory stream (see EvdevInputSourceTest), with
 * zero changes needed once Phase 5 supplies the real thing.
 */
class EvdevInputSource(
    private val stream: InputStream,
    private val onEvent: (RawInputEvent) -> Unit,
    private val onStopped: (Throwable?) -> Unit = {},
) {
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null

    /** Starts the background read loop. Calling start() while already running is a no-op. */
    fun start() {
        if (running.getAndSet(true)) return
        thread = Thread({
            val buffer = ByteArray(EvdevCodes.STRUCT_INPUT_EVENT_SIZE)
            var failureCause: Throwable? = null
            try {
                while (running.get()) {
                    if (!readFully(buffer)) break // clean EOF
                    onEvent(EvdevEventParser.parseOne(buffer))
                }
            } catch (e: IOException) {
                // If we're the ones who asked to stop, close()'ing the stream
                // is expected to make a blocked read() throw — that's how a
                // blocking read gets interrupted, not a real failure. Only
                // report it as an error if we DIDN'T ask to stop.
                if (running.get()) failureCause = e
            } finally {
                running.set(false)
                onStopped(failureCause)
            }
        }, "K2Pad-EvdevInputSource")
        thread?.isDaemon = true
        thread?.start()
    }

    /**
     * Stops the read loop and closes the underlying stream. Safe to call
     * even if the loop already stopped on its own (e.g. device disconnect).
     */
    fun stop() {
        if (!running.getAndSet(false)) return
        try {
            stream.close()
        } catch (_: IOException) {
            // Already gone — nothing left to do.
        }
    }

    private fun readFully(buffer: ByteArray): Boolean {
        var totalRead = 0
        while (totalRead < buffer.size) {
            val n = stream.read(buffer, totalRead, buffer.size - totalRead)
            if (n < 0) return false // EOF
            totalRead += n
        }
        return true
    }
}
