package com.chameleonultra.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chameleonultra.android.ui.MainViewModel
import com.chameleonultra.android.ui.components.AttackProgressIndicator
import com.chameleonultra.android.ui.components.KeyItem

/**
 * Screen for key recovery attacks (mfkey32, nested, darkside).
 */
@Composable
fun KeyRecoveryScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val recoveryState by viewModel.keyRecoveryState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Key Recovery",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Attack type selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Attack",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.startMfKey32() },
                        enabled = recoveryState !is MainViewModel.KeyRecoveryState.Running,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("mfkey32", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = { viewModel.startNested() },
                        enabled = recoveryState !is MainViewModel.KeyRecoveryState.Running,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Nested", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = { viewModel.startDarkside() },
                        enabled = recoveryState !is MainViewModel.KeyRecoveryState.Running,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Darkside", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress / results
        when (val state = recoveryState) {
            is MainViewModel.KeyRecoveryState.Idle -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Select an attack type to recover keys.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is MainViewModel.KeyRecoveryState.Running -> {
                AttackProgressIndicator(
                    title = state.title,
                    progress = state.progress,
                    status = state.status
                )
            }
            is MainViewModel.KeyRecoveryState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Keys Recovered",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        state.keys.forEach { (sector, keys) ->
                            Text(
                                text = "Sector $sector",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            KeyItem("Key A", keys.first)
                            KeyItem("Key B", keys.second)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
            is MainViewModel.KeyRecoveryState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
