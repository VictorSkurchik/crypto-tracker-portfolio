package by.vsdev.cpt.core.designsystem

import androidx.compose.runtime.Composable

/**
 * Foldable hinge/posture is an Android-only OS concept (via `androidx.window`) — Desktop and iOS
 * have no equivalent, so their actuals always report [None].
 */
sealed interface CptFoldPosture {
    data object None : CptFoldPosture

    /** Device is half-opened (tabletop/book posture) with a hinge splitting the window in two. */
    data class HalfOpened(
        val isHingeVertical: Boolean,
        val hingeStartFraction: Float,
        val hingeEndFraction: Float,
    ) : CptFoldPosture
}

@Composable
expect fun rememberFoldPosture(): CptFoldPosture
