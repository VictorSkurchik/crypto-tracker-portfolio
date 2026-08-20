package by.vsdev.cpt.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.cpt.core.designsystem.CptUnderlineTextField
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp))
            Text(
                "These platform-level keys are used to price your holdings and read EVM chain balances.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 28.dp),
            )
            CptUnderlineTextField(
                value = state.coinMarketCapApiKey,
                onValueChange = viewModel::setCoinMarketCapApiKey,
                label = { Text("CoinMarketCap API key") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Get a free key at coinmarketcap.com/api",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
            )
            CptUnderlineTextField(
                value = state.etherscanApiKey,
                onValueChange = viewModel::setEtherscanApiKey,
                label = { Text("Etherscan API key") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Get a free key at etherscan.io/apis",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
