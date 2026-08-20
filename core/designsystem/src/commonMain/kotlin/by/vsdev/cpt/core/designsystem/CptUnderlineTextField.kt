package by.vsdev.cpt.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Underline-only text field (no box outline), matching the design's bare bottom-border inputs.
 * Mirrors [androidx.compose.material3.OutlinedTextField]'s common param surface so call sites are
 * a mechanical swap.
 */
@Composable
fun CptUnderlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
        singleLine = true,
        shape = RectangleShape,
        colors =
            TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                errorIndicatorColor = MaterialTheme.colorScheme.error,
            ),
    )
}

/**
 * [CptUnderlineTextField] for secrets (API secret, passphrase): masked by default with a show/hide
 * toggle, since — unlike a login password — there's no muscle memory to fall back on if a pasted
 * exchange credential is wrong.
 */
@Composable
fun CptPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by remember { mutableStateOf(false) }
    CptUnderlineTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        keyboardActions = keyboardActions,
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                CptEyeGlyph(open = visible)
            }
        },
    )
}

private const val EYE_WIDTH_FRACTION = 0.6f
private const val EYE_PUPIL_RADIUS_FRACTION = 0.14f
private const val EYE_STROKE_WIDTH_FRACTION = 0.06f

/** Minimal hand-drawn eye / eye-with-a-slash glyph for the password show/hide toggle. */
@Composable
private fun CptEyeGlyph(open: Boolean) {
    val color = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val strokeWidth = size.minDimension * EYE_STROKE_WIDTH_FRACTION
        val eyeWidth = size.minDimension * EYE_WIDTH_FRACTION
        val center = Offset(size.width / 2f, size.height / 2f)
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - eyeWidth / 2f, center.y - eyeWidth / 2f),
            size = Size(eyeWidth, eyeWidth),
            style = Stroke(width = strokeWidth),
        )
        if (open) {
            drawCircle(color, radius = size.minDimension * EYE_PUPIL_RADIUS_FRACTION, center = center)
        } else {
            drawLine(
                color,
                Offset(center.x - eyeWidth / 2f, center.y - eyeWidth / 2f),
                Offset(center.x + eyeWidth / 2f, center.y + eyeWidth / 2f),
                strokeWidth,
            )
        }
    }
}
