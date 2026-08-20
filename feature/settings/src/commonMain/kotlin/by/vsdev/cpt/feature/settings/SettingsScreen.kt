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
import crypto_portfolio_tracker.feature.settings.generated.resources.Res
import crypto_portfolio_tracker.feature.settings.generated.resources.settings_cmc_api_key_field
import crypto_portfolio_tracker.feature.settings.generated.resources.settings_cmc_helper_text
import crypto_portfolio_tracker.feature.settings.generated.resources.settings_description
import crypto_portfolio_tracker.feature.settings.generated.resources.settings_etherscan_api_key_field
import crypto_portfolio_tracker.feature.settings.generated.resources.settings_etherscan_helper_text
import crypto_portfolio_tracker.feature.settings.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            Text(
                stringResource(Res.string.settings_title),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            )
            Text(
                stringResource(Res.string.settings_description),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 28.dp),
            )
            CptUnderlineTextField(
                value = state.coinMarketCapApiKey,
                onValueChange = viewModel::setCoinMarketCapApiKey,
                label = { Text(stringResource(Res.string.settings_cmc_api_key_field)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(Res.string.settings_cmc_helper_text),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
            )
            CptUnderlineTextField(
                value = state.etherscanApiKey,
                onValueChange = viewModel::setEtherscanApiKey,
                label = { Text(stringResource(Res.string.settings_etherscan_api_key_field)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(Res.string.settings_etherscan_helper_text),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
