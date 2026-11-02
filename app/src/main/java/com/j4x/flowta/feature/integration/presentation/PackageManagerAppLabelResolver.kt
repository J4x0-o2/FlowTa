package com.j4x.flowta.feature.integration.presentation

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Needs `<queries>` entries in the manifest for the source apps (package visibility, Android 11+). */
class PackageManagerAppLabelResolver @Inject constructor(
    @ApplicationContext private val context: Context
) : AppLabelResolver {

    override fun label(packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }
}
