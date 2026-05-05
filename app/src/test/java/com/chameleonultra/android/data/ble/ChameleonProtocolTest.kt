package com.chameleonultra.android.data.ble

import com.chameleonultra.android.domain.model.ChameleonFrame
import com.chameleonultra.android.domain.model.ChameleonCommands
import org.junit.Test
import org.junit.Assert.*

class ChameleonProtocolTest {

    @Test
    fun testBuildFrame() {
        // Тест: GET_APP_VERSION (0x1000) без данных
        val frame = ChameleonProtocol.buildFrame(0x1000, 0x0000, byteArrayOf())
        
        // Минимальный кадр: 10 байт (без данных)
        assertEquals(10, frame.size)
        assertEquals(ChameleonProtocol.SOF, frame[0])
        
        // Проверяем LRC1 (LRC над SOF)
        val expectedLrc1 = ChameleonProtocol.calculateLrc(frame, 0, 1)
        assertEquals(expectedLrc1, frame[1])
        
        // Проверяем CMD (big-endian: 0x1000)
        assertEquals(0x10.toByte(), frame[2])
        assertEquals(0x00.toByte(), frame[3])
        
        // Проверяем STATUS (0x0000)
        assertEquals(0x00.toByte(), frame[4])
        assertEquals(0x00.toByte(), frame[5])
        
        // Проверяем LEN (0x0000)
        assertEquals(0x00.toByte(), frame[6])
        assertEquals(0x00.toByte(), frame[7])
        
        // Проверяем LRC2 (LRC над CMD|STATUS|LEN)
        val expectedLrc2 = ChameleonProtocol.calculateLrc(frame, 2, 8)
        assertEquals(expectedLrc2, frame[8])
        
        // Проверяем LRC3 (LRC над DATA — пусто, так что LRC над ничем)
        val expectedLrc3 = ChameleonProtocol.calculateLrc(frame, 9, 9)
        assertEquals(expectedLrc3, frame[9])
    }

    @Test
    fun testBuildFrameWithData() {
        // Тест: команда с данными
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val frame = ChameleonProtocol.buildFrame(0x2000, 0x0000, data)
        
        // Кадр: 10 + 4 = 14 байт
        assertEquals(14, frame.size)
        
        // Проверяем LEN
        assertEquals(0x00.toByte(), frame[6])
        assertEquals(0x04.toByte(), frame[7])
        
        // Проверяем данные
        assertTrue(frame.slice(9..12).toByteArray().contentEquals(data))
    }

    @Test
    fun testParseFrame() {
        val data = byteArrayOf(0xAB.toByte(), 0xCD.toByte())
        val frame = ChameleonProtocol.buildFrame(0x1001, 0x0000, data)
        
        val parsed = ChameleonProtocol.parseFrame(frame)
        
        assertNotNull(parsed)
        assertEquals(0x1001, parsed!!.command)
        assertEquals(0x0000, parsed.status)
        assertTrue(parsed.data.contentEquals(data))
    }

    @Test
    fun testParseFrameWithOffset() {
        // Кадр в середине буфера
        val data = byteArrayOf(0x01, 0x02)
        val frame = ChameleonProtocol.buildFrame(0x2000, 0x0000, data)
        val buffer = ByteArray(5) + frame + ByteArray(3)
        
        val parsed = ChameleonProtocol.parseFrame(buffer, 5, frame.size)
        
        assertNotNull(parsed)
        assertEquals(0x2000, parsed!!.command)
    }

    @Test
    fun testInvalidFrame() {
        // Неверный SOF
        val invalidFrame = byteArrayOf(0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val parsed = ChameleonProtocol.parseFrame(invalidFrame)
        
        assertNull(parsed)
    }

    @Test
    fun testInvalidLrc() {
        // Правильный кадр, но с испорченным LRC
        val frame = ChameleonProtocol.buildFrame(0x1000, 0x0000, byteArrayOf())
        frame[8] = 0xFF.toByte() // портим LRC2
        
        val parsed = ChameleonProtocol.parseFrame(frame)
        assertNull(parsed)
    }

    @Test
    fun testFindFrame() {
        val frame = ChameleonProtocol.buildFrame(0x1000, 0x0000, byteArrayOf())
        val buffer = ByteArray(2) + frame + ByteArray(3)
        
        val (found, nextOffset) = ChameleonProtocol.findFrame(buffer)
        
        assertNotNull(found)
        assertEquals(0x1000, found!!.command)
        assertEquals(2 + frame.size, nextOffset)
    }

    @Test
    fun testFindFrameWithGarbage() {
        // Мусор перед кадром
        val frame = ChameleonProtocol.buildFrame(0x1000, 0x0000, byteArrayOf(0x01))
        val garbage = byteArrayOf(0xFF, 0xFE, 0xFD)
        val buffer = garbage + frame
        
        val (found, nextOffset) = ChameleonProtocol.findFrame(buffer)
        
        assertNotNull(found)
        assertEquals(0x1000, found!!.command)
        assertTrue(found.data.contentEquals(byteArrayOf(0x01)))
        assertEquals(garbage.size + frame.size, nextOffset)
    }

    @Test
    fun testLrcCalculation() {
        // Тест LRC: сумма [0x01, 0x02, 0x03] = 0x06, LRC = (0x100 - 0x06) & 0xFF = 0xFA
        val data = byteArrayOf(0x01, 0x02, 0x03)
        val lrc = ChameleonProtocol.calculateLrc(data)
        assertEquals(0xFA.toByte(), lrc)
        
        // Проверка: сумма + LRC = 0x100 (модulo 0x100 = 0x00)
        val sum = (0x01 + 0x02 + 0x03 + (lrc.toInt() and 0xFF)) and 0xFF
        assertEquals(0x00, sum)
    }

    @Test
    fun testBytesToHex() {
        val bytes = byteArrayOf(0x01, 0xAB, 0xCD, 0xFF.toByte())
        val hex = ChameleonProtocol.bytesToHex(bytes)
        assertEquals("01 AB CD FF", hex)
    }

    @Test
    fun testGetAppVersionFrame() {
        // Реальный тест: GET_APP_VERSION = 0x1000
        val frame = ChameleonProtocol.buildFrame(ChameleonCommands.GET_APP_VERSION, 0x0000, byteArrayOf())
        
        assertEquals(10, frame.size)
        
        // Парсим обратно
        val parsed = ChameleonProtocol.parseFrame(frame)
        assertNotNull(parsed)
        assertEquals(ChameleonCommands.GET_APP_VERSION, parsed!!.command)
        assertEquals(0x0000, parsed.status)
        assertEquals(0, parsed.data.size)
    }
}
