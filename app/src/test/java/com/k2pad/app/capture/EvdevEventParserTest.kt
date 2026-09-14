package com.k2pad.app.capture

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EvdevEventParserTest {

    /**
     * Hand-builds the exact 24-byte wire format verified against a real
     * struct input_event on a 64-bit system (see EvdevCodes's class doc):
     * 16 bytes of timestamp (contents irrelevant here, zero-filled), then
     * little-endian u16 type, u16 code, s32 value.
     */
    private fun buildRawEventBytes(type: Int, code: Int, value: Int): ByteArray {
        val buffer = ByteBuffer.allocate(EvdevCodes.STRUCT_INPUT_EVENT_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putLong(0L) // tv_sec
        buffer.putLong(0L) // tv_usec
        buffer.putShort(type.toShort())
        buffer.putShort(code.toShort())
        buffer.putInt(value)
        return buffer.array()
    }

    @Test
    fun `parses a key-down event`() {
        val bytes = buildRawEventBytes(EvdevCodes.EV_KEY, EvdevCodes.KEY_W, EvdevCodes.KEY_STATE_DOWN)
        val event = EvdevEventParser.parseOne(bytes)
        assertEquals(EvdevCodes.EV_KEY, event.type)
        assertEquals(EvdevCodes.KEY_W, event.code)
        assertEquals(EvdevCodes.KEY_STATE_DOWN, event.value)
    }

    @Test
    fun `parses a negative relative-motion value correctly (signed)`() {
        val bytes = buildRawEventBytes(EvdevCodes.EV_REL, EvdevCodes.REL_X, -37)
        val event = EvdevEventParser.parseOne(bytes)
        assertEquals(-37, event.value)
    }

    @Test
    fun `parses a code above the signed-short boundary correctly (unsigned u16)`() {
        // Worst-case-shaped value right at the u16 boundary, to guard the
        // masking logic even though no code K2Pad currently binds is this high.
        val bytes = buildRawEventBytes(EvdevCodes.EV_KEY, 0xFFFF, EvdevCodes.KEY_STATE_UP)
        val event = EvdevEventParser.parseOne(bytes)
        assertEquals(65535, event.code)
    }

    @Test
    fun `parses multiple consecutive events from one buffer at their correct offsets`() {
        val one = buildRawEventBytes(EvdevCodes.EV_KEY, EvdevCodes.KEY_A, EvdevCodes.KEY_STATE_DOWN)
        val two = buildRawEventBytes(EvdevCodes.EV_SYN, EvdevCodes.SYN_REPORT, 0)
        val combined = one + two

        val first = EvdevEventParser.parseOne(combined, offset = 0)
        val second = EvdevEventParser.parseOne(combined, offset = EvdevCodes.STRUCT_INPUT_EVENT_SIZE)

        assertEquals(EvdevCodes.KEY_A, first.code)
        assertEquals(EvdevCodes.EV_SYN, second.type)
        assertEquals(EvdevCodes.SYN_REPORT, second.code)
    }

    @Test
    fun `rejects a buffer that is too short`() {
        var threw = false
        try {
            EvdevEventParser.parseOne(ByteArray(10))
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        org.junit.Assert.assertTrue("expected an IllegalArgumentException for a too-short buffer", threw)
    }
}
