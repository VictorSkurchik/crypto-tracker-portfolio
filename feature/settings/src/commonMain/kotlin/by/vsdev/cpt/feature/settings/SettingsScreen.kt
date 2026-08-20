package by.vsdev.cpt.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                "These platform-level keys are used to price your holdings and read EVM chain balances — " +
                    "get a free key from coinmarketcap.com/api and etherscan.io/apis.",
            )
            OutlinedTextField(
                value = state.coinMarketCapApiKey,
                onValueChange = viewModel::setCoinMarketCapApiKey,
                label = { Text("CoinMarketCap API key") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            OutlinedTextField(
                value = state.etherscanApiKey,
                onValueChange = viewModel::setEtherscanApiKey,
                label = { Text("Etherscan API key") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}
