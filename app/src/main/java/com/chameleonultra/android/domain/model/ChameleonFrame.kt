package com.chameleonultra.android.domain.model

/**
 * Модель бинарного кадра протокола ChameleonUltra
 *
 * Формат кадра:
 * [SOF: 1 byte = 0x11]
 * [LRC1: 1 byte]
 * [CMD: 2 bytes, big-endian]
 * [STATUS: 2 bytes, big-endian]
 * [LEN: 2 bytes, big-endian]
 * [LRC2: 1 byte]
 * [DATA: LEN bytes]
 * [LRC3: 1 byte]
 *
 * @param command — 16-bit код команды (например, 0x1000 = GET_APP_VERSION)
 * @param status — 16-bit статус (0x0000 от клиента, от устройства — результат)
 * @param data — payload переменной длины (макс 512 байт)
 */
data class ChameleonFrame(
    val command: Int,
    val status: Int,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChameleonFrame
        if (command != other.command) return false
        if (status != other.status) return false
        if (!data.contentEquals(other.data)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = command
        result = 31 * result + status
        result = 31 * result + data.contentHashCode()
        return result
    }

    /**
     * Получить command в виде hex-строки
     */
    fun commandHex(): String = "0x${command.toString(16).uppercase().padStart(4, '0')}"

    /**
     * Получить status в виде hex-строки
     */
    fun statusHex(): String = "0x${status.toString(16).uppercase().padStart(4, '0')}"
}
