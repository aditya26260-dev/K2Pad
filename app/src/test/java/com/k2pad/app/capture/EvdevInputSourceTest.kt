package com.k2pad.app.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class EvdevInputSourceTest {

    private fun buildRawEventBytes(type: Int, code: Int, value: Int): ByteArray {
        val buffer = ByteBuffer.allocate(EvdevCodes.STRUCT_INPUT_EVENT_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putLong(0L)
        buffer.putLong(0L)
        buffer.putShort(type.toShort())
        buffer.putShort(code.toShort())
        buffer.putInt(value)
        return buffer.array()
    }

    /**
     * A fully self-controlled test double whose read() blocks until
     * close() is called, then throws — used instead of a real pipe/socket
     * stream because relying on a generic JDK stream's real-world
     * close()-unblocks-a-pending-read behavior is a known inconsistent
     * area across stream implementations; this way the test verifies
     * EvdevInputSource's own logic, not some other class's threading
     * quirks.
     */
    private class BlockUntilClosedStream : InputStream() {
        @Volatile private var closed = false

        // Deliberately java.lang.Object, not Kotlin's Any: Kotlin's Any
        // does not expose wait()/notifyAll() (verified by trying — Kotlin
        // hides those particular inherited Object members on purpose),
        // and this test double genuinely wants classic monitor wait/notify.
        private val lock = java.lang.Object()

        override fun read(): Int = throw UnsupportedOperationException("use the 3-arg read")

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            synchronized(lock) {
                while (!closed) {
                    lock.wait(50)
                }
            }
            throw IOException("stream closed")
        }

        override fun close() {
            synchronized(lock) {
                closed = true
                lock.notifyAll()
            }
        }
    }

    @Test
    fun `reads every event out of a finite stream and reports a clean stop at EOF`() {
        val bytes = buildRawEventBytes(EvdevCodes.EV_KEY, EvdevCodes.KEY_W, EvdevCodes.KEY_STATE_DOWN) +
            buildRawEventBytes(EvdevCodes.EV_SYN, EvdevCodes.SYN_REPORT, 0)

        val received = CopyOnWriteArrayList<RawInputEvent>()
        val stoppedLatch = CountDownLatch(1)
        var stopCause: Throwable? = null

        val source = EvdevInputSource(
            stream = ByteArrayInputStream(bytes),
            onEvent = { received.add(it) },
            onStopped = { cause -> stopCause = cause; stoppedLatch.countDown() },
        )

        source.start()
        val finished = stoppedLatch.await(2, TimeUnit.SECONDS)

        assertTrue("read loop should have stopped on its own at EOF", finished)
        assertEquals(2, received.size)
        assertEquals(EvdevCodes.KEY_W, received[0].code)
        assertEquals(EvdevCodes.SYN_REPORT, received[1].code)
        assertEquals(null, stopCause)
    }

    @Test
    fun `stop() closes the stream and the loop exits without being treated as an error`() {
        val stoppedLatch = CountDownLatch(1)
        var stopCause: Throwable? = null
        val blockingStream = BlockUntilClosedStream()

        val source = EvdevInputSource(
            stream = blockingStream,
            onEvent = { },
            onStopped = { cause -> stopCause = cause; stoppedLatch.countDown() },
        )

        source.start()
        Thread.sleep(100) // let the loop actually enter its blocking read()
        source.stop()

        val finished = stoppedLatch.await(2, TimeUnit.SECONDS)
        assertTrue("read loop should exit promptly once stop() closes the stream", finished)
        assertEquals("an intentional stop should not be reported as an error", null, stopCause)
    }

    @Test
    fun `calling start twice does not spawn a second reader`() {
        val bytes = buildRawEventBytes(EvdevCodes.EV_SYN, EvdevCodes.SYN_REPORT, 0)
        val received = CopyOnWriteArrayList<RawInputEvent>()
        val stoppedLatch = CountDownLatch(1)

        val source = EvdevInputSource(
            stream = ByteArrayInputStream(bytes),
            onEvent = { received.add(it) },
            onStopped = { stoppedLatch.countDown() },
        )

        source.start()
        source.start() // should be a no-op
        stoppedLatch.await(2, TimeUnit.SECONDS)

        assertEquals(1, received.size)
    }
}
