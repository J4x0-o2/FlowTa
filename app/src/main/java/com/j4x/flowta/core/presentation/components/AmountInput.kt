package com.j4x.flowta.core.presentation.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.format.AmountVisualTransformation
import com.j4x.flowta.core.presentation.format.parseAmount
import com.j4x.flowta.core.presentation.format.sanitizeAmountInput

/**
 * Money text field that shows the thousands separator inside the field while typing
 * (`373929` is displayed as `373,929`). The value passed to [onValueChange] stays raw (digits and,
 * for USD/EUR, one "." decimal mark), so callers keep gating submit with
 * `parseAmount(text, code) != null`. The rule follows the reactive [LocalCurrencyCode].
 */
@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "0",
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val code = LocalCurrencyCode.current
    val symbol = LocalCurrencySymbol.current
    // parseAmount reads a trailing decimal mark ("12.") as 12, so submit and this hint agree.
    val invalid = parseAmount(value, code) == null

    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(sanitizeAmountInput(it, code)) },
        visualTransformation = remember { AmountVisualTransformation() },
        label = label?.let { { Text(it) } },
        placeholder = { Text(placeholder, style = textStyle) },
        textStyle = textStyle,
        prefix = { Text(symbol) },
        isError = invalid,
        supportingText = if (invalid) ({ Text("Monto no válido o ambiguo") }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}
