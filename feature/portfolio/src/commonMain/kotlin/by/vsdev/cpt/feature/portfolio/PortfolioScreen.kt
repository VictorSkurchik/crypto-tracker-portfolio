package by.vsdev.cpt.feature.portfolio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.cpt.core.designsystem.CptBadgeShape
import by.vsdev.cpt.core.designsystem.CptCoinBadge
import by.vsdev.cpt.core.model.AccountBreakdown
import by.vsdev.cpt.core.model.AssetBreakdown
import by.vsdev.cpt.core.model.ProviderError
import crypto_portfolio_tracker.feature.portfolio.generated.resources.Res
import crypto_portfolio_tracker.feature.portfolio.generated.resources.portfolio_by_account_header
import crypto_portfolio_tracker.feature.portfolio.generated.resources.portfolio_by_asset_header
import crypto_portfolio_tracker.feature.portfolio.generated.resources.portfolio_empty_state
import crypto_portfolio_tracker.feature.portfolio.generated.resources.portfolio_not_updated_yet
import crypto_portfolio_tracker.feature.portfolio.generated.resources.portfolio_refreshing
import crypto_portfolio_tracker.feature.portfolio.generated.resources.portfolio_sync_failed
import crypto_portfolio_tracker.feature.portfolio.generated.resources.portfolio_total_value_label
import crypto_portfolio_tracker.feature.portfolio.generated.resources.portfolio_updated_days_ago
import crypto_portfolio_tracker.feature.portfolio.generated.resources.portfolio_updated_hours_ago
import crypto_portfolio_tracker.feature.portfolio.generated.resources.portfolio_updated_just_now
import crypto_portfolio_tracker.feature.portfolio.generated.resources.portfolio_updated_minutes_ago
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Clock
import kotlin.time.Instant

private const val REFRESH_ARC_START_ANGLE_DEGREES = 0f
private const val REFRESH_ARC_SWEEP_ANGLE_DEGREES = 300f
private const val REFRESH_ARROWHEAD_SIZE_FACTOR = 0.34f
private const val REFRESH_ARROWHEAD_HALF_WIDTH_FACTOR = 0.6f
private const val HALF_TURN_DEGREES = 180f

private val chainBadgeIconSymbols =
    mapOf(
        "ETHEREUM" to "ETH",
        "OPTIMISM" to "OP",
        "ARBITRUM" to "ARB",
        "TON" to "TON",
        "TRON" to "TRX",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        val snapshot = state.snapshot
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = viewModel::refresh),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                stringResource(Res.string.portfolio_total_value_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(11.dp).padding(end = 6.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    RefreshGlyph(modifier = Modifier.padding(end = 6.dp))
                                }
                                Text(
                                    if (state.isRefreshing) {
                                        stringResource(Res.string.portfolio_refreshing)
                                    } else {
                                        snapshot?.lastUpdated?.let { formatLastUpdated(it) }
                                            ?: stringResource(Res.string.portfolio_not_updated_yet)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            "$${formatUsd(snapshot?.totalValueUsd ?: 0.0)}",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (snapshot == null || snapshot.byAccount.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(stringResource(Res.string.portfolio_empty_state))
                        }
                    }
                } else {
                    item {
                        Text(
                            stringResource(Res.string.portfolio_by_account_header),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                    items(snapshot.byAccount) { account ->
                        AccountRow(account, error = state.lastErrors[account.accountId.value])
                    }
                    if (snapshot.byAsset.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(Res.string.portfolio_by_asset_header),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        items(snapshot.byAsset) { asset -> AssetRow(asset) }
                    }
                }
            }
        }
    }
}

/**
 * A minimal circular-arrow glyph — same hand-drawn Canvas style as `CptNavIcons` — restoring a
 * small, discoverable tap affordance for [PortfolioViewModel.refresh] now that the header row no
 * longer has an icon of its own.
 */
@Composable
private fun RefreshGlyph(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier.size(12.dp)) {
        val strokeWidthPx = 1.6.dp.toPx()
        val diameter = size.minDimension - strokeWidthPx
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawArc(
            color = color,
            startAngle = REFRESH_ARC_START_ANGLE_DEGREES,
            sweepAngle = REFRESH_ARC_SWEEP_ANGLE_DEGREES,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
        )

        val endAngleDegrees = REFRESH_ARC_START_ANGLE_DEGREES + REFRESH_ARC_SWEEP_ANGLE_DEGREES
        val endAngleRad = endAngleDegrees * (kotlin.math.PI / HALF_TURN_DEGREES)
        val tip = Offset(center.x + radius * cos(endAngleRad).toFloat(), center.y + radius * sin(endAngleRad).toFloat())
        // Tangent of the arc at its end point (clockwise travel direction), used to orient the arrowhead.
        val tangent = Offset(-sin(endAngleRad).toFloat(), cos(endAngleRad).toFloat())
        val normal = Offset(-tangent.y, tangent.x)
        val arrowLength = diameter * REFRESH_ARROWHEAD_SIZE_FACTOR
        val arrowHalfWidth = arrowLength * REFRESH_ARROWHEAD_HALF_WIDTH_FACTOR
        val back = Offset(tip.x - tangent.x * arrowLength, tip.y - tangent.y * arrowLength)
        val left = Offset(back.x + normal.x * arrowHalfWidth, back.y + normal.y * arrowHalfWidth)
        val right = Offset(back.x - normal.x * arrowHalfWidth, back.y - normal.y * arrowHalfWidth)
        drawPath(
            path =
                Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(left.x, left.y)
                    lineTo(right.x, right.y)
                    close()
                },
            color = color,
        )
    }
}

@Composable
private fun AccountRow(
    account: AccountBreakdown,
    error: ProviderError?,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val badgeShape = if (account.badge == "EXCHANGE") CptBadgeShape.SQUARE else CptBadgeShape.CIRCLE
                CptCoinBadge(
                    chainBadgeIconSymbols[account.badge] ?: account.badge,
                    badgeShape,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    account.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    account.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .padding(start = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            Text(
                "$${formatUsd(account.valueUsd)}",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (error != null) {
            Text(
                stringResource(Res.string.portfolio_sync_failed, error.message),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun AssetRow(asset: AssetBreakdown) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CptCoinBadge(asset.assetSymbol, CptBadgeShape.CIRCLE, modifier = Modifier.padding(end = 8.dp))
            Text(
                "${asset.assetSymbol} · ${asset.quantity}",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            "$${formatUsd(asset.valueUsd)}",
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private const val CENTS_PER_DOLLAR = 100
private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24

private fun formatUsd(value: Double): String {
    val rounded =
        (value * CENTS_PER_DOLLAR).let { if (it < 0) kotlin.math.ceil(it) else kotlin.math.floor(it) } / CENTS_PER_DOLLAR
    return rounded.toString()
}

@Composable
private fun formatLastUpdated(lastUpdated: Instant): String {
    val elapsedSeconds = (Clock.System.now() - lastUpdated).inWholeSeconds.coerceAtLeast(0)
    val elapsedMinutes = elapsedSeconds / SECONDS_PER_MINUTE
    val elapsedHours = elapsedMinutes / MINUTES_PER_HOUR
    val elapsedDays = elapsedHours / HOURS_PER_DAY
    return when {
        elapsedSeconds < SECONDS_PER_MINUTE -> stringResource(Res.string.portfolio_updated_just_now)
        elapsedMinutes < MINUTES_PER_HOUR -> stringResource(Res.string.portfolio_updated_minutes_ago, elapsedMinutes)
        elapsedHours < HOURS_PER_DAY -> stringResource(Res.string.portfolio_updated_hours_ago, elapsedHours)
        else -> stringResource(Res.string.portfolio_updated_days_ago, elapsedDays)
    }
}
