package com.k2pad.app.capture

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parses raw bytes read from a /dev/input/eventX node into [RawInputEvent]s,
 * using the exact struct layout verified in [EvdevCodes]'s class doc.
 */
object EvdevEventParser {

    /**
     * Parses exactly one struct input_event out of [bytes] starting at
     * [offset]. [bytes] must contain at least
     * [EvdevCodes.STRUCT_INPUT_EVENT_SIZE] bytes from [offset] onward.
     *
     * `type` and `code` are unsigned 16-bit fields in the kernel struct;
     * they're read as signed shorts and then masked with 0xFFFF to
     * reconstruct the correct unsigned value regardless of whether the
     * raw bits would look negative as a signed Short. `value` is
     * genuinely signed (relative motion deltas can be negative) so it's
     * read as a plain 32-bit int with no masking.
     */
    fun parseOne(bytes: ByteArray, offset: Int = 0): RawInputEvent {
        require(bytes.size - offset >= EvdevCodes.STRUCT_INPUT_EVENT_SIZE) {
            "need at least ${EvdevCodes.STRUCT_INPUT_EVENT_SIZE} bytes from offset $offset, " +
                "got ${bytes.size - offset}"
        }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val type = buffer.getShort(offset + EvdevCodes.OFFSET_TYPE).toInt() and 0xFFFF
        val code = buffer.getShort(offset + EvdevCodes.OFFSET_CODE).toInt() and 0xFFFF
        val value = buffer.getInt(offset + EvdevCodes.OFFSET_VALUE)
        return RawInputEvent(type, code, value)
    }
}
