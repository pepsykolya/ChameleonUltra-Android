package com.chameleonultra.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chameleonultra.android.ui.MainViewModel
import com.chameleonultra.android.ui.components.AttackProgressIndicator

/**
 * Screen for emulating a MIFARE Classic card in a ChameleonUltra slot.
 */
@Composable
fun CardEmulateScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val cardState by viewModel.cardState.collectAsState()
    val isEmulating by viewModel.isEmulating.collectAsState()
    val selectedSlot by viewModel.selectedSlot.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Emulate Card",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Slot selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Slot",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (0..7).forEach { slot ->
                        Button(
                            onClick = { viewModel.selectSlot(slot) },
                            enabled = !isEmulating,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$slot",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Selected: Slot $selectedSlot",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isEmulating)
                    MaterialTheme.colorScheme.tertiaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (val state = cardState) {
                    is MainViewModel.CardState.Empty -> {
                        Text(
                            text = "No card loaded. Read a card first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is MainViewModel.CardState.Loaded -> {
                        val card = state.card
                        Text(
                            text = "Card: ${card.uidHex()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Type: ${card.type.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Sectors: ${card.sectors.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (isEmulating) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "● Emulating in slot $selectedSlot",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Emulate / Stop buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.startEmulation() },
                enabled = cardState is MainViewModel.CardState.Loaded && !isEmulating,
                modifier = Modifier.weight(1f)
            ) {
                Text("Start Emulation")
            }
            Button(
                onClick = { viewModel.stopEmulation() },
                enabled = isEmulating,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop Emulation")
            }
        }
    }
}
