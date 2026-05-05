package com.chameleonultra.android.domain.model

/**
 * Команды протокола ChameleonUltra (big-endian, 16-bit)
 *
 * Основано на официальном Python CLI:
 * https://github.com/RfidResearchGroup/ChameleonUltra/blob/main/software/script/chameleon_enum.py
 */
object ChameleonCommands {

    // === System Commands ===
    const val GET_APP_VERSION = 0x1000
    const val SET_DEVICE_MODE = 0x1001
    const val GET_DEVICE_MODE = 0x1002
    const val SET_SLOT_ACTIVATED = 0x1003
    const val GET_SLOT_ACTIVATED = 0x1004
    const val SET_SLOT_TAG_TYPE = 0x1005
    const val GET_SLOT_TAG_TYPE = 0x1006
    const val SET_SLOT_DATA_DEFAULT = 0x1007
    const val SET_SLOT_DATA_FIRST = 0x1008
    const val SET_SLOT_DATA_RESET = 0x1009
    const val GET_SLOT_DATA = 0x100A
    const val SET_SLOT_ENABLE = 0x100B
    const val GET_SLOT_ENABLE = 0x100C
    const val SET_SLOT_TAG_NICK = 0x100D
    const val GET_SLOT_TAG_NICK = 0x100E
    const val GET_SELECTED_SLOT = 0x100F
    const val SET_ANIMATION_MODE = 0x1010
    const val GET_ANIMATION_MODE = 0x1011
    const val SET_LONG_PRESS = 0x1012
    const val GET_LONG_PRESS = 0x1013
    const val SET_BLE_PAIRING_KEY = 0x1014
    const val GET_BLE_PAIRING_KEY = 0x1015
    const val DELETE_BLE_PAIRING_KEY = 0x1016
    const val GET_GIT_VERSION = 0x1017
    const val GET_DEVICE_CHIP_ID = 0x1018
    const val GET_DEVICE_ADDRESS = 0x1019
    const val SAVE_SETTINGS = 0x101A
    const val RESET_SETTINGS = 0x101B
    const val GET_BATTERY_INFO = 0x101C
    const val GET_BATTERY_VOLTAGE = 0x101D
    const val GET_SETTING = 0x101E
    const val SET_SETTING = 0x101F
    const val WIPE_ALL_FDS = 0x1020
    const val GET_FIRMWARE_VERSION = 0x1021

    // === LF (125kHz) Commands ===
    const val EM410X_SCAN = 0x2000
    const val EM410X_WRITE_TO_T55XX = 0x2001
    const val EM410X_READ_EM410X = 0x2002
    const val EM410X_WRITE_EM410X = 0x2003
    const val EM410X_LOAD_EM410X = 0x2004
    const val EM410X_SAVE_EM410X = 0x2005

    // === HF (13.56MHz) Commands ===
    const val HF14A_SCAN = 0x3000
    const val HF14A_RAW = 0x3001

    // === MIFARE Classic Commands ===
    const val MF1_READ_ONE_BLOCK = 0x4000
    const val MF1_WRITE_ONE_BLOCK = 0x4001
    const val MF1_READ_SECTOR = 0x4002
    const val MF1_WRITE_SECTOR = 0x4003
    const val MF1_SET_DETECTION_ENABLE = 0x4004
    const val MF1_GET_DETECTION_COUNT = 0x4005
    const val MF1_GET_DETECTION_LOG = 0x4006
    const val MF1_GET_DETECTION_ENABLE = 0x4007
    const val MF1_READ_ONE_BLOCK_V2 = 0x4008
    const val MF1_WRITE_ONE_BLOCK_V2 = 0x4009
    const val MF1_READ_SECTOR_V2 = 0x400A
    const val MF1_WRITE_SECTOR_V2 = 0x400B
    const val MF1_GET_EMULATOR_CONFIG = 0x400C
    const val MF1_GET_BLOCK_DATA = 0x400D
    const val MF1_SET_BLOCK_DATA = 0x400E
    const val MF1_GET_SECTOR_DATA = 0x400F
    const val MF1_SET_SECTOR_DATA = 0x4010
    const val MF1_GET_ANTICOLLISION_DATA = 0x4011
    const val MF1_SET_ANTICOLLISION_DATA = 0x4012
    const val MF1_GET_GEN1A_MODE = 0x4013
    const val MF1_SET_GEN1A_MODE = 0x4014
    const val MF1_GET_GEN2_MODE = 0x4015
    const val MF1_SET_GEN2_MODE = 0x4016
    const val MF1_GET_USE_FIRST_BLOCK = 0x4017
    const val MF1_SET_USE_FIRST_BLOCK = 0x4018
    const val MF1_GET_WRITE_MODE = 0x4019
    const val MF1_SET_WRITE_MODE = 0x401A
    const val MF1_GET_EMULATION_MODE = 0x401B
    const val MF1_SET_EMULATION_MODE = 0x401C
    const val MF1_GET_DETECTION_STATUS = 0x401D
    const val MF1_GET_DETECTION_RESULT = 0x401E

    // === Tag-specific Commands ===
    const val TAG_LOAD = 0x5000
    const val TAG_SAVE = 0x5001
    const val TAG_EMULATION_START = 0x5002
    const val TAG_EMULATION_STOP = 0x5003
    const val TAG_GET_DATA = 0x5004
    const val TAG_SET_DATA = 0x5005
    const val TAG_DELETE = 0x5006
    const val TAG_GET_INFO = 0x5007

    // === DFU ===
    const val ENTER_DFU = 0x6000
    const val DFU_FLASH = 0x6001
    const val DFU_VERIFY = 0x6002
    const val DFU_RESET = 0x6003

    // === Status Codes ===
    object Status {
        const val SUCCESS = 0x0000
        const val FAILURE = 0x0001
        const val INVALID_CMD = 0x0002
        const val INVALID_ARG = 0x0003
        const val INVALID_CFG = 0x0004
        const val NOT_IMPLEMENTED = 0x0005
        const val TIMEOUT = 0x0006
        const val HF_TAG_OK = 0x0100
        const val HF_TAG_NO = 0x0101
        const val HF_ERR_STAT = 0x0102
        const val HF_ERR_CRC = 0x0103
        const val HF_ERR_AUTH = 0x0104
        const val HF_ERR_PARITY = 0x0105
        const val HF_ERR_BCC = 0x0106
        const val HF_ERR_COLLISION = 0x0107
        const val LF_TAG_OK = 0x0200
        const val LF_TAG_NO = 0x0201
    }

    // === Tag Types ===
    object TagType {
        const val MIFARE_1024 = 0x1001
        const val MIFARE_2048 = 0x1002
        const val MIFARE_4096 = 0x1003
        const val MIFARE_MINI = 0x1004
        const val NFC_T2T = 0x2001
        const val EM410X = 0x3001
    }
}
