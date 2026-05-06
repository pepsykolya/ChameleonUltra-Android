package com.chameleonultra.android.data.ble

import com.chameleonultra.android.domain.model.ChameleonFrame

object ChameleonProtocol {
    const val SOF: Byte = 0x11
    const val EOF: Byte = 0x11
    const val LRC1: Byte = 0xEF.toByte()

    fun buildFrame(cmd: Int, status: Int = 0, data: ByteArray = byteArrayOf()): ByteArray {
        val len = data.size
        val frame = mutableListOf<Byte>()
        frame.add(SOF)
        frame.add(LRC1)
        frame.add((cmd shr 8).toByte())
        frame.add((cmd and 0xFF).toByte())
        frame.add((status shr 8).toByte())
        frame.add((status and 0xFF).toByte())
        frame.add((len shr 8).toByte())
        frame.add((len and 0xFF).toByte())
        val lrc2 = computeLrc(frame.subList(2, frame.size).toByteArray())
        frame.add(lrc2)
        data.forEach { frame.add(it) }
        val lrc3 = computeLrc(data)
        frame.add(lrc3)
        frame.add(EOF)
        return frame.toByteArray()
    }

    fun parseFrame(bytes: ByteArray): ChameleonFrame? {
        if (bytes.size < 10) return null
        if (bytes[0] != SOF || bytes[1] != LRC1) return null
        val cmd = ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[3].toInt() and 0xFF)
        val status = ((bytes[4].toInt() and 0xFF) shl 8) or (bytes[5].toInt() and 0xFF)
        val len = ((bytes[6].toInt() and 0xFF) shl 8) or (bytes[7].toInt() and 0xFF)
        if (bytes.size < 10 + len) return null
        val lrc2 = bytes[8]
        val data = bytes.copyOfRange(9, 9 + len)
        val lrc3 = bytes[9 + len]
        val eof = bytes[10 + len]
        if (eof != EOF) return null
        val computedLrc2 = computeLrc(bytes.copyOfRange(2, 8))
        val computedLrc3 = computeLrc(data)
        if (lrc2 != computedLrc2 || lrc3 != computedLrc3) return null
        return ChameleonFrame(cmd, status, data)
    }

    fun findFrame(buffer: ByteArray): Pair<ChameleonFrame?, ByteArray> {
        val sofIndex = buffer.indexOf(SOF)
        if (sofIndex == -1 || buffer.size < sofIndex + 10) return null to buffer
        val potentialFrame = buffer.copyOfRange(sofIndex, buffer.size)
        val frame = parseFrame(potentialFrame)
        return if (frame != null) {
            val consumed = 10 + frame.data.size + 1
            frame to buffer.copyOfRange(sofIndex + consumed, buffer.size)
        } else {
            null to buffer.copyOfRange(sofIndex + 1, buffer.size)
        }
    }

    private fun computeLrc(data: ByteArray): Byte {
        var sum = 0
        data.forEach { sum += it.toInt() and 0xFF }
        return ((-sum) and 0xFF).toByte()
    }

    fun bytesToHex(bytes: ByteArray): String = bytes.joinToString(" ") { "%02X".format(it) }
}
