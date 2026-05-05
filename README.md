# ChameleonUltra Android

A custom Android application for the [ChameleonUltra](https://github.com/RfidResearchGroup/ChameleonUltra) v2.0 that enables cloning MIFARE Classic cards **without leaking keys to third-party apps**.

## Features

- **BLE Connection** — Connect to ChameleonUltra via Bluetooth Low Energy (Nordic UART Service)
- **MIFARE Classic Card Reading** — Read card sectors and keys directly through the device
- **mfkey32 Key Recovery** — Recover unknown keys using the mfkey32v2 algorithm with detection logs
- **Nested Attack** — Nested authentication attack for hardened MIFARE Classic cards
- **Darkside Attack** — Darkside attack for super-hardened cards
- **Card Emulation** — Emulate cloned cards in ChameleonUltra slots
- **Local-First Security** — All keys stay on your device; nothing is sent to external servers

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM |
| DI | Hilt |
| BLE | Nordic UART Service (NUS) |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |

## Setup

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android device with BLE support (Android 7.0+)
- ChameleonUltra firmware v2.0+

### Build

```bash
git clone https://github.com/pepsykolya/ChameleonUltra-Android.git
cd ChameleonUltra-Android
./gradlew assembleDebug
```

Or open in Android Studio and click **Run**.

## Usage

1. **Scan** — Press "Scan" to find nearby ChameleonUltra devices
2. **Connect** — Tap a device to connect via BLE
3. **Read Card** — Place a MIFARE Classic card on the ChameleonUltra and press "Read Card"
4. **Recover Keys** (if needed) — Use mfkey32, nested, or darkside attacks to recover unknown keys
5. **Save** — Save the card dump to a ChameleonUltra slot
6. **Emulate** — Select a slot and start emulation

## Architecture

```
┌─────────────────────────────────────┐
│              UI Layer               │
│   (Compose Screens + ViewModel)   │
├─────────────────────────────────────┤
│            Domain Layer             │
│  (UseCases: MfKey32, Nested, etc) │
├─────────────────────────────────────┤
│             Data Layer              │
│    (BLE Manager + Protocol)        │
├─────────────────────────────────────┤
│         ChameleonUltra              │
│      (BLE Nordic UART)              │
└─────────────────────────────────────┘
```

### Key Components

- `BleManager` — Handles BLE scanning, connection, and Nordic UART communication
- `ChameleonProtocol` — Binary protocol parser (SOF=0x11, LRC, 16-bit BE frames)
- `MfKey32Calculator` — mfkey32v2 key recovery algorithm
- `NestedAttack` / `DarksideAttack` — Hardcard attack implementations
- `CardReadScreen` / `CardEmulateScreen` / `KeyRecoveryScreen` — Compose UI screens

## Protocol

The app uses the official ChameleonUltra binary protocol:

```
[SOF: 0x11] [LRC1] [CMD: 2 BE] [STATUS: 2 BE] [LEN: 2 BE] [LRC2] [DATA: LEN] [LRC3] [EOF: 0x11]
```

Command constants are defined in `ChameleonCommands.kt` based on the [official Python CLI](https://github.com/RfidResearchGroup/ChameleonUltra/blob/main/software/script/chameleon_enum.py).

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/my-feature`
3. Commit your changes: `git commit -m "feat: add something"`
4. Push to the branch: `git push origin feat/my-feature`
5. Open a Pull Request

Please ensure your code follows the existing Kotlin style and includes tests where applicable.

## License

MIT License — see [LICENSE](LICENSE) for details.

## Links

- [ChameleonUltra Repository](https://github.com/RfidResearchGroup/ChameleonUltra)
- [ChameleonUltra Documentation](https://github.com/RfidResearchGroup/ChameleonUltra/tree/main/docs)
- [mfkey32v2 by equipter](https://github.com/equipter/mfkey)
- [MIFARE Classic Security](https://www.cs.ru.nl/~rverdult/Cards_Mifare_Classic_-_Security_and_Privacy(2012-03-15).pdf)

---

**Disclaimer:** This tool is for educational and authorized security testing purposes only. Always comply with local laws and obtain proper authorization before testing any card system.
