package com.j4x.flowta.feature.integration.presentation

/** Resolves a user-facing app name for a package; falls back to the package name itself. */
interface AppLabelResolver {
    fun label(packageName: String): String
}

/** A linked source app, with referenced names resolved; a null name means the target was deleted. */
data class ConnectedApp(
    val sourceApp: String,
    val label: String,
    val typeName: String?,
    val cashAccountName: String?,
    val transferAccountName: String?
) {
    val needsReconnect: Boolean
        get() = typeName == null || cashAccountName == null || transferAccountName == null
}

data class ConnectedAppsState(
    val apps: List<ConnectedApp> = emptyList(),
    val pendingDisconnect: ConnectedApp? = null
)

sealed interface ConnectedAppsIntent {
    data class RequestDisconnect(val sourceApp: String) : ConnectedAppsIntent
    data object DismissDisconnect : ConnectedAppsIntent
    data object ConfirmDisconnect : ConnectedAppsIntent
}

sealed interface ConnectedAppsEffect {
    data class ShowError(val message: String) : ConnectedAppsEffect
}
