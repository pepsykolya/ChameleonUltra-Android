package com.chameleonultra.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chameleonultra.android.domain.card.MifareCard
import com.chameleonultra.android.ui.components.SectorCard
import com.chameleonultra.android.ui.MainViewModel

/**
 * Screen for reading a MIFARE Classic card.
 */
@Composable
fun CardReadScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val cardState by viewModel.cardState.collectAsState()
    val isReading by viewModel.isReading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Read Card",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Card info header
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
                when (val state = cardState) {
                    is MainViewModel.CardState.Empty -> {
                        Text(
                            text = "No card read yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    is MainViewModel.CardState.Loading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Reading sector ${state.currentSector}/${state.totalSectors}...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is MainViewModel.CardState.Error -> {
                        Text(
                            text = "Error: ${state.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is MainViewModel.CardState.Loaded -> {
                        CardInfo(card = state.card)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Read button
        Button(
            onClick = { viewModel.startCardRead() },
            enabled = !isReading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isReading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Read Card")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sectors list
        if (cardState is MainViewModel.CardState.Loaded) {
            val card = (cardState as MainViewModel.CardState.Loaded).card
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(card.sectors) { sector ->
                    SectorCard(sector = sector)
                }
            }
        }
    }
}

@Composable
private fun CardInfo(card: MifareCard) {
    Column {
        Text(
            text = "UID: ${card.uidHex()}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "SAK: ${"%02X".format(card.sak)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "ATQA: ${card.atqaHex()}",
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
    }
}
