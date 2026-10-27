package com.j4x.flowta.core.presentation.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.format.Formatters
import com.j4x.flowta.core.presentation.format.parseAmount

/**
 * Money text field that shows how the typed text is interpreted ("Monto: $5,000") or
 * "Monto no válido o ambiguo". The rule follows the reactive [LocalCurrencyCode].
 * Callers gate their submit button with `parseAmount(text, code) != null`.
 */
@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "0",
    previewLabel: String = "Monto",
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val code = LocalCurrencyCode.current
    val symbol = LocalCurrencySymbol.current
    val parsed = parseAmount(value, code)
    val invalid = parsed == null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        placeholder = { Text(placeholder, style = textStyle) },
        textStyle = textStyle,
        prefix = { Text(symbol) },
        isError = invalid,
        supportingText = {
            when {
                invalid -> Text("Monto no válido o ambiguo")
                value.isNotBlank() -> Text("$previewLabel: $symbol${Formatters.amount(parsed!!, code)}")
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}
