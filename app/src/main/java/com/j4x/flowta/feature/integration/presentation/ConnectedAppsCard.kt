package com.j4x.flowta.feature.integration.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.j4x.flowta.R
import com.j4x.flowta.core.presentation.theme.flowColors

@Composable
fun ConnectedAppsCard(
    apps: List<ConnectedApp>,
    onDisconnect: (sourceApp: String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            apps.forEachIndexed { index, app ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ConnectedAppRow(app, onDisconnect = { onDisconnect(app.sourceApp) })
            }
        }
    }
}

@Composable
private fun ConnectedAppRow(app: ConnectedApp, onDisconnect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(app.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onDisconnect) {
                Text(stringResource(R.string.connected_apps_disconnect), color = flowColors.expenseText)
            }
        }
        DetailLine(prefix = "", name = app.typeName, deleted = stringResource(R.string.connected_apps_type_deleted))
        DetailLine(
            prefix = stringResource(R.string.connected_apps_cash_prefix),
            name = app.cashAccountName,
            deleted = stringResource(R.string.connected_apps_account_deleted)
        )
        DetailLine(
            prefix = stringResource(R.string.connected_apps_transfer_prefix),
            name = app.transferAccountName,
            deleted = stringResource(R.string.connected_apps_account_deleted)
        )
        if (app.needsReconnect) {
            Text(
                stringResource(R.string.connected_apps_reconnect_hint, app.label),
                style = MaterialTheme.typography.bodySmall,
                color = flowColors.expenseText
            )
        }
    }
}

/** Renders `prefix + name`, or `prefix + deleted` in the error color when the target no longer exists. */
@Composable
private fun DetailLine(prefix: String, name: String?, deleted: String) {
    val error = flowColors.expenseText
    Text(
        text = buildAnnotatedString {
            append(prefix)
            if (name != null) append(name) else withStyle(SpanStyle(color = error)) { append(deleted) }
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}

@Composable
fun DisconnectAppDialog(app: ConnectedApp, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.connected_apps_disconnect_title, app.label)) },
        text = { Text(stringResource(R.string.connected_apps_disconnect_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.connected_apps_disconnect), color = flowColors.expenseText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.connected_apps_cancel)) }
        }
    )
}
