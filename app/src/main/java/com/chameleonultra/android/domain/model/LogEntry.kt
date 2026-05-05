package com.chameleonultra.android.domain.model

sealed class LogEntry {
    abstract val timestamp: Long

    data class Command(
        override val timestamp: Long,
        val bytes: ByteArray
    ) : LogEntry() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Command
            if (timestamp != other.timestamp) return false
            if (!bytes.contentEquals(other.bytes)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = timestamp.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }

    data class Response(
        override val timestamp: Long,
        val bytes: ByteArray
    ) : LogEntry() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Response
            if (timestamp != other.timestamp) return false
            if (!bytes.contentEquals(other.bytes)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = timestamp.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }

    data class Info(
        override val timestamp: Long,
        val message: String
    ) : LogEntry()

    data class Error(
        override val timestamp: Long,
        val message: String
    ) : LogEntry()
}
