package com.chameleonultra.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chameleonultra.android.domain.model.ChameleonCommands
import com.chameleonultra.android.domain.model.BleDevice
import com.chameleonultra.android.domain.model.ConnectionState
import com.chameleonultra.android.domain.model.LogEntry
import com.chameleonultra.android.ui.screens.CardEmulateScreen
import com.chameleonultra.android.ui.screens.CardReadScreen
import com.chameleonultra.android.ui.screens.KeyRecoveryScreen
import com.chameleonultra.android.ui.theme.ChameleonUltraTheme

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val devices by viewModel.discoveredDevices.collectAsState()
    val logs by viewModel.logs.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        "Device" to Icons.Default.Home,
        "Read" to Icons.Default.Nfc,
        "Emulate" to Icons.Default.Settings,
        "Keys" to Icons.Default.Keyboard
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> DeviceTab(
                    connectionState = connectionState,
                    devices = devices,
                    logs = logs,
                    viewModel = viewModel
                )
                1 -> CardReadScreen(viewModel)
                2 -> CardEmulateScreen(viewModel)
                3 -> KeyRecoveryScreen(viewModel)
            }
        }
    }
}

@Composable
fun DeviceTab(
    connectionState: ConnectionState,
    devices: List<BleDevice>,
    logs: List<LogEntry>,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        StatusSection(connectionState)

        Spacer(modifier = Modifier.height(16.dp))

        ControlSection(
            connectionState = connectionState,
            onScan = viewModel::startScan,
            onStopScan = viewModel::stopScan,
            onDisconnect = viewModel::disconnect
        )

        Spacer(modifier = Modifier.height(16.dp))

        DevicesSection(
            devices = devices,
            connectionState = connectionState,
            onConnect = viewModel::connect
        )

        Spacer(modifier = Modifier.height(16.dp))

        LogsSection(logs = logs)
    }
}


@Composable
fun StatusSection(connectionState: ConnectionState) {
    val (statusText, statusColor) = when (connectionState) {
        is ConnectionState.Disconnected -> "Disconnected" to MaterialTheme.colorScheme.error
        is ConnectionState.Scanning -> "Scanning..." to MaterialTheme.colorScheme.primary
        is ConnectionState.Connecting -> "Connecting..." to MaterialTheme.colorScheme.tertiary
        is ConnectionState.Connected -> "Connected" to MaterialTheme.colorScheme.secondary
        is ConnectionState.Error -> "Error" to MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Status: $statusText",
                style = MaterialTheme.typography.titleMedium,
                color = statusColor
            )
            if (connectionState is ConnectionState.Scanning) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ControlSection(
    connectionState: ConnectionState,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (connectionState) {
            is ConnectionState.Disconnected, is ConnectionState.Error -> {
                Button(
                    onClick = onScan,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Scan")
                }
            }
            is ConnectionState.Scanning -> {
                Button(
                    onClick = onStopScan,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop Scan")
                }
            }
            is ConnectionState.Connected -> {
                Button(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disconnect")
                }
            }
            else -> {}
        }
    }
}

@Composable
fun CommandSection(
    viewModel: MainViewModel,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Commands",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.sendCommand(ChameleonCommands.GET_APP_VERSION) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                Text("Get Version", style = MaterialTheme.typography.bodySmall)
            }
            
            Button(
                onClick = { viewModel.sendCommand(ChameleonCommands.GET_DEVICE_CHIP_ID) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                Text("Chip ID", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.sendCommand(ChameleonCommands.MF1_SET_DETECTION_ENABLE, data = byteArrayOf(0x01)) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                Text("Detect ON", style = MaterialTheme.typography.bodySmall)
            }
            
            Button(
                onClick = { viewModel.sendCommand(ChameleonCommands.MF1_GET_DETECTION_LOG) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                Text("Get Logs", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun DevicesSection(
    devices: List<BleDevice>,
    connectionState: ConnectionState,
    onConnect: (BleDevice) -> Unit
) {
    Text(
        text = "Devices",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    if (devices.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No devices found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.height(200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices) { device ->
                DeviceItem(
                    device = device,
                    enabled = connectionState is ConnectionState.Scanning || connectionState is ConnectionState.Disconnected,
                    onConnect = { onConnect(device) }
                )
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: BleDevice,
    enabled: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onConnect,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = when (device) {
                    is BleDevice.ChameleonUltra -> "Ultra"
                    is BleDevice.ChameleonLite -> "Lite"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LogsSection(logs: List<LogEntry>) {
    Text(
        text = "Logs",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(logs) { log ->
                LogItem(log = log)
                if (log != logs.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun LogItem(log: LogEntry) {
    val (prefix, color) = when (log) {
        is LogEntry.Command -> "→ CMD" to MaterialTheme.colorScheme.primary
        is LogEntry.Response -> "← RSP" to MaterialTheme.colorScheme.secondary
        is LogEntry.Info -> "ℹ INFO" to MaterialTheme.colorScheme.onSurfaceVariant
        is LogEntry.Error -> "✕ ERR" to MaterialTheme.colorScheme.error
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$prefix ${log.timestamp}",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        Text(
            text = when (log) {
                is LogEntry.Command -> log.bytes.joinToString(" ") { String.format("%02X", it) }
                is LogEntry.Response -> log.bytes.joinToString(" ") { String.format("%02X", it) }
                is LogEntry.Info -> log.message
                is LogEntry.Error -> log.message
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
